package com.carepilot.chatworkbench.service;

import com.carepilot.chatworkbench.dto.response.ConversationResponse;
import com.carepilot.chatworkbench.entity.AiDialogStepRecord;
import com.carepilot.chatworkbench.entity.Conversation;
import com.carepilot.chatworkbench.enums.PlatformEnum;
import com.carepilot.chatworkbench.repository.AiDialogStepRecordRepository;
import com.carepilot.chatworkbench.repository.ConversationRepository;
import com.carepilot.chatworkbench.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIService {

    private final ConversationRepository conversationRepository;
    private final AiDialogStepRecordRepository stepRecordRepository;
    private final AiDialogStepAssembler stepAssembler;
    private final DialogContextBuilder dialogContextBuilder;
    private final ConversationService conversationService;
    private final TokenService tokenService;
    private final DeepSeekClient deepSeekClient;
    private final IntentRecognitionPromptService promptService;
    private final ReplyStrategyRuleService replyStrategyRuleService;

    @Value("${app.ai.timeout-seconds:15}")
    private Integer timeoutSeconds;

    private final ExecutorService executorService = Executors.newFixedThreadPool(4);

    public ConversationResponse createConversation(String customerId, String question,
                                                    String platform, String customerType) {
        assertTokenQuota(customerId);

        Conversation conversation = Conversation.builder()
                .conversationId(IdGenerator.generateConversationId())
                .customerId(customerId)
                .platform(platform)
                .title(truncate(question, 50))
                .build();
        conversationRepository.saveAndFlush(conversation);

        List<AiDialogStepRecord> historyRecords = loadHistory(conversation.getId());
        int dialogRound = stepRecordRepository.findMaxDialogRound(conversation.getId()) + 1;
        String extraJson = stepAssembler.createExtraJson(
                customerType,
                "意图识别阶段仅做分析，不直接回复客户。",
                "请确认意图识别结果后再进入下一步骤。");
        try {
            StepGenerationResult result = generateCurrentStep(
                    1, question, platform, dialogRound, historyRecords, null, null);
            saveSuccessfulStep(
                    conversation, customerId, question, dialogRound, 1, 1,
                    extraJson, result);
        } catch (RuntimeException exception) {
            saveFailedStep(
                    conversation, customerId, question, dialogRound, 1, 1,
                    extraJson, exception);
            throw exception;
        }
        return conversationService.getConversationById(customerId, conversation.getConversationId());
    }

    public ConversationResponse addMessage(String customerId, String conversationId,
                                           String question, String customerType) {
        assertTokenQuota(customerId);

        Conversation conversation = conversationRepository.findByConversationId(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation does not exist"));
        if (!customerId.equals(conversation.getCustomerId())) {
            throw new SecurityException("Access to this conversation is forbidden");
        }

        List<AiDialogStepRecord> historyRecords = loadHistory(conversation.getId());
        int dialogRound = stepRecordRepository.findMaxDialogRound(conversation.getId()) + 1;
        String extraJson = stepAssembler.createExtraJson(
                customerType,
                "意图识别阶段仅做分析，不直接回复客户。",
                "请确认意图识别结果后再进入下一步骤。");
        try {
            StepGenerationResult result = generateCurrentStep(
                    1, question, conversation.getPlatform(), dialogRound, historyRecords, null, null);
            saveSuccessfulStep(
                    conversation, customerId, question, dialogRound, 1, 1,
                    extraJson, result);
        } catch (RuntimeException exception) {
            saveFailedStep(
                    conversation, customerId, question, dialogRound, 1, 1,
                    extraJson, exception);
            throw exception;
        }
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);
        return conversationService.getConversationById(customerId, conversationId);
    }

    public ConversationResponse generateStep(String customerId, String conversationId,
                                             Integer dialogRound, Integer stepNo) {
        return generateStep(customerId, conversationId, dialogRound, stepNo, null);
    }

    public ConversationResponse generateStep(String customerId, String conversationId,
                                             Integer dialogRound, Integer stepNo, String carrierName) {
        assertTokenQuota(customerId);
        validateDialogStep(dialogRound, stepNo);
        if (stepNo == 1) {
            throw new IllegalArgumentException("第1步必须通过提交新的客户问题触发");
        }

        Conversation conversation = requireOwnedConversation(customerId, conversationId);
        List<AiDialogStepRecord> versions = stepRecordRepository
                .findBySessionTaskIdAndDialogRoundAndStepNoAndIsDeleteOrderByStepRoundDesc(
                        conversation.getId(), dialogRound, stepNo.byteValue(), (byte) 0);
        boolean alreadyGenerated = versions.stream()
                .anyMatch(record -> Byte.valueOf((byte) 1).equals(record.getIsEffective()));
        if (alreadyGenerated) {
            throw new IllegalStateException("该步骤已生成，请使用重新生成功能");
        }

        int stepRound = versions.stream()
                .map(AiDialogStepRecord::getStepRound)
                .filter(value -> value != null)
                .max(Integer::compareTo)
                .orElse(0) + 1;
        List<AiDialogStepRecord> historyRecords = loadHistory(conversation.getId());
        AiDialogStepRecord previousStep = historyRecords.stream()
                .filter(record -> dialogRound.equals(record.getDialogRound()))
                .filter(record -> record.getStepNo() != null
                        && record.getStepNo().intValue() == stepNo - 1)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("请先生成并确认上一步内容"));

        try {
            StepGenerationResult result = generateCurrentStep(
                    stepNo, previousStep.getCustomerDialog(), conversation.getPlatform(),
                    dialogRound, historyRecords, null, carrierName);
            String carrierMetadata = replyStrategyRuleService.carrierMetadata(carrierName);
            saveSuccessfulStep(
                    conversation, customerId, previousStep.getCustomerDialog(), dialogRound,
                    stepNo, stepRound, stepNo == 2 && carrierMetadata != null
                            ? carrierMetadata : previousStep.getExtraJson(), result);
        } catch (RuntimeException exception) {
            saveFailedStep(
                    conversation, customerId, previousStep.getCustomerDialog(), dialogRound,
                    stepNo, stepRound, previousStep.getExtraJson(), exception);
            throw exception;
        }

        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);
        return conversationService.getConversationById(customerId, conversationId);
    }

    public ConversationResponse regenerateStep(String customerId, String conversationId,
                                               Integer dialogRound, Integer stepNo) {
        return regenerateStep(customerId, conversationId, dialogRound, stepNo, null);
    }

    public ConversationResponse regenerateStep(String customerId, String conversationId,
                                               Integer dialogRound, Integer stepNo, String carrierName) {
        assertTokenQuota(customerId);
        validateDialogStep(dialogRound, stepNo);
        Conversation conversation = requireOwnedConversation(customerId, conversationId);

        List<AiDialogStepRecord> versions = stepRecordRepository
                .findBySessionTaskIdAndDialogRoundAndStepNoAndIsDeleteOrderByStepRoundDesc(
                        conversation.getId(), dialogRound, stepNo.byteValue(), (byte) 0);
        if (versions.isEmpty()) {
            throw new IllegalArgumentException("当前对话轮次不存在该步骤的生成记录");
        }

        AiDialogStepRecord current = versions.stream()
                .filter(record -> Byte.valueOf((byte) 1).equals(record.getIsEffective()))
                .findFirst()
                .orElse(versions.getFirst());
        int nextStepRound = versions.stream()
                .map(AiDialogStepRecord::getStepRound)
                .filter(value -> value != null)
                .max(Integer::compareTo)
                .orElse(0) + 1;
        List<AiDialogStepRecord> historyRecords = loadHistory(conversation.getId());

        StepGenerationResult result;
        String rememberedCarrier = carrierName == null || carrierName.isBlank()
                ? replyStrategyRuleService.carrierFromMetadata(current.getExtraJson())
                : carrierName;
        try {
            result = generateCurrentStep(
                    stepNo, current.getCustomerDialog(), conversation.getPlatform(),
                    dialogRound, historyRecords, current.getAiContent(), rememberedCarrier);
        } catch (RuntimeException exception) {
            saveFailedStep(
                    conversation, customerId, current.getCustomerDialog(), current.getDialogRound(),
                    current.getStepNo().intValue(), nextStepRound,
                    current.getExtraJson(), exception);
            throw exception;
        }

        current.setIsEffective((byte) 0);
        String carrierMetadata = replyStrategyRuleService.carrierMetadata(rememberedCarrier);
        AiDialogStepRecord regenerated = copyRegeneratedRecord(
                conversation, customerId, current, nextStepRound, result,
                stepNo == 2 && carrierMetadata != null ? carrierMetadata : current.getExtraJson());
        stepRecordRepository.saveAll(List.of(current, regenerated));
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);
        return conversationService.getConversationById(customerId, conversationId);
    }

    private void saveSuccessfulStep(Conversation conversation, String customerId, String question,
                                    int dialogRound, int stepNo, int stepRound, String extraJson,
                                    StepGenerationResult result) {
        LocalDateTime now = LocalDateTime.now();
        String storedContext = dialogContextBuilder.buildStoredStepContext(
                loadStoredContextHistory(conversation.getId()),
                question, dialogRound, stepNo, stepRound, result.content());
        stepRecordRepository.save(AiDialogStepRecord.builder()
                .sessionTaskId(conversation.getId())
                .platformId(platformId(conversation.getPlatform()))
                .customerDialog(question)
                .dialogContext(storedContext)
                .dialogRound(dialogRound)
                .stepNo((byte) stepNo)
                .stepRound(stepRound)
                .aiContent(result.content())
                .extraJson(extraJson)
                .isManualEdit((byte) 0)
                .isEffective((byte) 1)
                .triggerAt(now)
                .llmReplyAt(now)
                .generateStatus((byte) 1)
                .operatorId(Long.valueOf(customerId))
                .traceId(result.requestId() == null || result.requestId().isBlank()
                        ? UUID.randomUUID().toString()
                        : truncate(result.requestId(), 128))
                .isDelete((byte) 0)
                .promptTokens(result.promptTokens())
                .completionTokens(result.completionTokens())
                .totalTokens(result.totalTokens())
                .build());
    }

    private StepGenerationResult generateCurrentStep(Integer stepNo, String question, String platform,
                                                     Integer dialogRound,
                                                     List<AiDialogStepRecord> historyRecords,
                                                     String previousContent, String carrierName) {
        CompletableFuture<StepGenerationResult> future = CompletableFuture.supplyAsync(() -> {
            if (stepNo == 1) {
                String prompt = promptService.render(
                        platform,
                        extractKeywords(question),
                        question,
                        dialogContextBuilder.buildConversationContext(historyRecords));
                DeepSeekClient.DeepSeekResult response = deepSeekClient.recognizeIntent(prompt);
                promptService.validateOutput(response.content());
                return StepGenerationResult.from(response);
            }

            String generationContext = buildStepGenerationContext(
                    dialogRound, stepNo, historyRecords, previousContent);
            if (stepNo == 2) {
                String intentRecognition = historyRecords.stream()
                        .filter(record -> dialogRound.equals(record.getDialogRound()))
                        .filter(record -> record.getStepNo() != null && record.getStepNo().intValue() == 1)
                        .map(AiDialogStepRecord::getAiContent)
                        .findFirst().orElse("");
                ReplyStrategyRuleService.ReplyStrategyPlan plan = replyStrategyRuleService.plan(
                        platform, question, intentRecognition, carrierName);
                DeepSeekClient.DeepSeekResult response = deepSeekClient.complete(plan.prompt(),
                        "服务平台：" + platform + "\n\n客户当前问题：" + question
                                + "\n\n意图识别及上下文：\n" + generationContext
                                + "\n\n请只输出第2步回复策略正文。");
                ReplyStrategyRuleService.ValidationResult validation = replyStrategyRuleService.validate(response.content(), plan);
                if (!validation.valid()) {
                    response = deepSeekClient.complete(plan.prompt() + "\n上次输出未通过校验："
                                    + validation.message() + "。请严格修正后重新输出。",
                            "服务平台：" + platform + "\n\n客户当前问题：" + question
                                    + "\n\n意图识别及上下文：\n" + generationContext);
                    validation = replyStrategyRuleService.validate(response.content(), plan);
                }
                if (!validation.valid()) {
                    throw new IllegalStateException("回复策略未通过业务校验：" + validation.message());
                }
                return StepGenerationResult.from(response);
            }
            DeepSeekClient.DeepSeekResult response = deepSeekClient.complete(
                    stepSystemPrompt(stepNo),
                    "服务平台：" + platform + "\n\n客户当前问题：" + question
                            + "\n\n当前对话及前序步骤：\n" + generationContext
                            + "\n\n请重新生成第" + stepNo + "步内容，只输出本步骤正文。"
            );
            if (response.content() == null || response.content().isBlank()) {
                throw new DeepSeekClient.DeepSeekApiException("DeepSeek 返回了空的步骤生成结果");
            }
            return StepGenerationResult.from(response);
        }, executorService);

        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (Exception exception) {
            future.cancel(true);
            log.error("Step {} regeneration timeout or failure after {} seconds",
                    stepNo, timeoutSeconds, exception);
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException("当前步骤重新生成失败", cause);
        }
    }

    private AiDialogStepRecord copyRegeneratedRecord(Conversation conversation, String customerId,
                                                      AiDialogStepRecord current, int nextStepRound,
                                                      StepGenerationResult result, String extraJson) {
        LocalDateTime now = LocalDateTime.now();
        String storedContext = dialogContextBuilder.buildStoredStepContext(
                loadStoredContextHistory(conversation.getId()),
                current.getCustomerDialog(), current.getDialogRound(),
                current.getStepNo().intValue(), nextStepRound, result.content());
        return AiDialogStepRecord.builder()
                .sessionTaskId(conversation.getId())
                .platformId(current.getPlatformId())
                .customerDialog(current.getCustomerDialog())
                .dialogContext(storedContext)
                .dialogRound(current.getDialogRound())
                .stepNo(current.getStepNo())
                .stepRound(nextStepRound)
                .aiContent(result.content())
                .extraJson(extraJson)
                .isManualEdit((byte) 0)
                .isEffective((byte) 1)
                .triggerAt(now)
                .llmReplyAt(now)
                .generateStatus((byte) 1)
                .operatorId(Long.valueOf(customerId))
                .traceId(result.requestId() == null || result.requestId().isBlank()
                        ? UUID.randomUUID().toString()
                        : truncate(result.requestId(), 128))
                .isDelete((byte) 0)
                .promptTokens(result.promptTokens())
                .completionTokens(result.completionTokens())
                .totalTokens(result.totalTokens())
                .build();
    }

    private void saveFailedStep(Conversation conversation, String customerId, String question,
                                int dialogRound, int stepNo, int stepRound,
                                String extraJson, RuntimeException exception) {
        try {
            String storedContext = dialogContextBuilder.buildStoredStepContext(
                    loadStoredContextHistory(conversation.getId()),
                    question, dialogRound, stepNo, stepRound, null);
            AiDialogStepRecord failed = AiDialogStepRecord.builder()
                    .sessionTaskId(conversation.getId())
                    .platformId(platformId(conversation.getPlatform()))
                    .customerDialog(question)
                    .dialogContext(storedContext)
                    .dialogRound(dialogRound)
                    .stepNo((byte) stepNo)
                    .stepRound(stepRound)
                    .extraJson(extraJson)
                    .isManualEdit((byte) 0)
                    .isEffective((byte) 0)
                    .triggerAt(LocalDateTime.now())
                    .generateStatus((byte) 2)
                    .failMsg(truncate(errorMessage(exception), 255))
                    .operatorId(Long.valueOf(customerId))
                    .traceId(UUID.randomUUID().toString())
                    .isDelete((byte) 0)
                    .promptTokens(0)
                    .completionTokens(0)
                    .totalTokens(0)
                    .build();
            stepRecordRepository.save(failed);
        } catch (RuntimeException persistenceException) {
            log.error("Failed to persist DeepSeek generation failure", persistenceException);
        }
    }

    private String buildStepGenerationContext(Integer dialogRound, Integer stepNo,
                                              List<AiDialogStepRecord> historyRecords,
                                              String previousContent) {
        StringBuilder context = new StringBuilder();
        historyRecords.stream()
                .filter(record -> dialogRound.equals(record.getDialogRound()))
                .filter(record -> record.getStepNo() != null && record.getStepNo() < stepNo)
                .sorted(java.util.Comparator.comparing(AiDialogStepRecord::getStepNo))
                .forEach(record -> context.append("第").append(record.getStepNo())
                        .append("步-").append(stepDescription(record.getStepNo()))
                        .append("：").append(record.getAiContent()).append("\n"));
        if (previousContent != null && !previousContent.isBlank()) {
            context.append("上一次本步骤输出：").append(previousContent).append("\n")
                    .append("本次需要在遵循事实的前提下给出不同且更优的版本。\n");
        }
        return context.isEmpty() ? "无" : context.toString().trim();
    }

    private String stepSystemPrompt(Integer stepNo) {
        return switch (stepNo) {
            case 2 -> "你是电商客服回复策略助手。根据客户问题和意图识别结果，生成简明、可执行的回复策略；只分析策略，不直接编造客户事实，不输出Markdown标题。";
            case 3 -> "你是电商客服话术助手。根据客户问题、意图和回复策略，生成一段可直接发送给客户的专业友好话术；不得虚构优惠、库存、物流或售后承诺。";
            case 4 -> "你是电商客服钩子引导助手。根据前序内容生成1至3个自然的引导问题，用于继续了解需求，不强推、不虚构事实。";
            case 5 -> "你是电商客服成功收尾助手。根据完整上下文生成简洁、友好的收尾内容，明确下一步但不得作出未经证实的承诺。";
            default -> throw new IllegalArgumentException("当前步骤不支持通用生成");
        };
    }

    private String stepDescription(Byte stepNo) {
        return switch (stepNo) {
            case 1 -> "意图识别";
            case 2 -> "回复策略";
            case 3 -> "推荐话术";
            case 4 -> "钩子引导";
            case 5 -> "成功收尾";
            default -> "未知步骤";
        };
    }

    private List<AiDialogStepRecord> loadHistory(Long sessionTaskId) {
        return stepRecordRepository
                .findBySessionTaskIdAndIsEffectiveAndIsDeleteOrderByDialogRoundAscStepNoAsc(
                        sessionTaskId, (byte) 1, (byte) 0);
    }

    private List<AiDialogStepRecord> loadStoredContextHistory(Long sessionTaskId) {
        return stepRecordRepository
                .findBySessionTaskIdAndIsDeleteOrderByDialogRoundAscStepNoAscStepRoundAsc(
                        sessionTaskId, (byte) 0)
                .stream()
                .filter(record -> Byte.valueOf((byte) 1).equals(record.getGenerateStatus()))
                .filter(record -> record.getAiContent() != null && !record.getAiContent().isBlank())
                .toList();
    }

    private void validateDialogStep(Integer dialogRound, Integer stepNo) {
        if (dialogRound == null || dialogRound < 1) {
            throw new IllegalArgumentException("dialogRound 必须大于 0");
        }
        if (stepNo == null || stepNo < 1 || stepNo > 5) {
            throw new IllegalArgumentException("stepNo 必须在 1 到 5 之间");
        }
    }

    private Conversation requireOwnedConversation(String customerId, String conversationId) {
        Conversation conversation = conversationRepository.findByConversationId(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation does not exist"));
        if (!customerId.equals(conversation.getCustomerId())) {
            throw new SecurityException("Access to this conversation is forbidden");
        }
        return conversation;
    }

    private void assertTokenQuota(String customerId) {
        if (tokenService.isTokenQuotaExceeded(customerId)) {
            throw new IllegalStateException("今日Token配额已用尽");
        }
    }

    private Long platformId(String platform) {
        PlatformEnum[] platforms = PlatformEnum.values();
        for (int index = 0; index < platforms.length; index++) {
            PlatformEnum candidate = platforms[index];
            if (candidate.name().equalsIgnoreCase(platform)
                    || candidate.getDescription().equalsIgnoreCase(platform)) {
                return (long) index + 1;
            }
        }
        return null;
    }

    private String extractKeywords(String question) {
        String normalized = question
                .replaceAll("[\\p{Punct}，。！？、；：\\s]+", " ")
                .trim();
        return normalized.isBlank() ? "未提取到明确关键词" : truncate(normalized, 100);
    }

    private String errorMessage(Throwable throwable) {
        return throwable.getMessage() == null || throwable.getMessage().isBlank()
                ? throwable.getClass().getSimpleName()
                : throwable.getMessage();
    }

    private String truncate(String text, int maxLength) {
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }

    private record StepGenerationResult(
            String content,
            Integer promptTokens,
            Integer completionTokens,
            Integer totalTokens,
            String requestId
    ) {
        static StepGenerationResult from(DeepSeekClient.DeepSeekResult response) {
            return new StepGenerationResult(
                    response.content(),
                    response.promptTokens(),
                    response.completionTokens(),
                    response.totalTokens(),
                    response.requestId());
        }
    }
}
