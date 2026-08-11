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
import java.util.concurrent.TimeoutException;

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
    private final ReplyStrategyPromptService replyStrategyPromptService;
    private final RecommendedScriptPromptService recommendedScriptPromptService;
    private final HookAndClosingPromptService hookAndClosingPromptService;

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
                    1, question, platform, dialogRound, historyRecords, null);
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
                    1, question, conversation.getPlatform(), dialogRound, historyRecords, null);
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
                    dialogRound, historyRecords, null);
            saveSuccessfulStep(
                    conversation, customerId, previousStep.getCustomerDialog(), dialogRound,
                    stepNo, stepRound, previousStep.getExtraJson(), result);
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
        try {
            result = generateCurrentStep(
                    stepNo, current.getCustomerDialog(), conversation.getPlatform(),
                    dialogRound, historyRecords, current.getAiContent());
        } catch (RuntimeException exception) {
            saveFailedStep(
                    conversation, customerId, current.getCustomerDialog(), current.getDialogRound(),
                    current.getStepNo().intValue(), nextStepRound,
                    current.getExtraJson(), exception);
            throw exception;
        }

        current.setIsEffective((byte) 0);
        AiDialogStepRecord regenerated = copyRegeneratedRecord(
                conversation, customerId, current, nextStepRound, result,
                current.getExtraJson());
        stepRecordRepository.saveAll(List.of(current, regenerated));
        tokenService.recordAiUsage(regenerated, conversation.getConversationId(), result.model());
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);
        return conversationService.getConversationById(customerId, conversationId);
    }

    public ConversationResponse updateStepContent(String customerId, String conversationId,
                                                  Integer dialogRound, Integer stepNo, String content) {
        validateDialogStep(dialogRound, stepNo);
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("编辑内容不能为空");
        }
        Conversation conversation = requireOwnedConversation(customerId, conversationId);

        List<AiDialogStepRecord> versions = stepRecordRepository
                .findBySessionTaskIdAndDialogRoundAndStepNoAndIsDeleteOrderByStepRoundDesc(
                        conversation.getId(), dialogRound, stepNo.byteValue(), (byte) 0);
        AiDialogStepRecord current = versions.stream()
                .filter(record -> Byte.valueOf((byte) 1).equals(record.getIsEffective()))
                .findFirst()
                .orElse(null);
        if (current == null) {
            throw new IllegalStateException("当前步骤尚无已生成内容，无法编辑");
        }

        current.setAiContent(content);
        current.setIsManualEdit((byte) 1);
        String storedContext = dialogContextBuilder.buildStoredStepContext(
                loadStoredContextHistory(conversation.getId()),
                current.getCustomerDialog(), current.getDialogRound(),
                current.getStepNo().intValue(), current.getStepRound(), content);
        current.setDialogContext(storedContext);
        stepRecordRepository.save(current);
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
        AiDialogStepRecord saved = stepRecordRepository.save(AiDialogStepRecord.builder()
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
        tokenService.recordAiUsage(saved, conversation.getConversationId(), result.model());
    }

    private StepGenerationResult generateCurrentStep(Integer stepNo, String question, String platform,
                                                     Integer dialogRound,
                                                     List<AiDialogStepRecord> historyRecords,
                                                     String previousContent) {
        CompletableFuture<StepGenerationResult> future = CompletableFuture.supplyAsync(() -> {
            if (stepNo == 1) {
                String prompt = promptService.render(
                        platform,
                        extractKeywords(question),
                        question,
                        dialogContextBuilder.buildConversationContext(historyRecords, dialogRound));
                DeepSeekClient.DeepSeekResult response = deepSeekClient.recognizeIntent(prompt);
                promptService.validateOutput(response.content());
                return StepGenerationResult.from(response);
            }

            String generationContext = buildStepGenerationContext(
                    dialogRound, stepNo, historyRecords, previousContent);
            if (stepNo == 2) {
                String userPrompt = "服务平台：" + platform
                        + "\n\n客户当前问题：" + question
                        + "\n\n当前轮意图识别及重新生成参考：\n" + generationContext
                        + "\n\n请严格按照回复策略提示词规定的结构输出第2步内容。"
                        + "只输出回复策略分析，不直接生成发送给客户的话术。";
                DeepSeekClient.DeepSeekResult response = deepSeekClient.complete(
                        replyStrategyPromptService.render(), userPrompt);
                replyStrategyPromptService.validateOutput(response.content());
                return StepGenerationResult.from(response);
            }
            if (stepNo == 3) {
                String userPrompt = "服务平台：" + platform
                        + "\n\n客户当前问题：" + question
                        + "\n\n当前对话、意图识别及回复策略：\n" + generationContext
                        + "\n\n请严格按“直接复制话术（平台版）”和“话术点评”两部分输出第3步内容。"
                        + "推荐话术必须能直接发送给客户，点评只说明表达设计；不得补充输入中不存在的商品或服务事实。";
                DeepSeekClient.DeepSeekResult response = deepSeekClient.complete(
                        recommendedScriptPromptService.render(platform), userPrompt);
                recommendedScriptPromptService.validateOutput(response.content());
                return StepGenerationResult.from(response);
            }
            if (stepNo == 4 || stepNo == 5) {
                String userPrompt = "服务平台：" + platform
                        + "\n\n客户当前问题：" + question
                        + "\n\n当前对话及全部前序步骤：\n" + generationContext
                        + "\n\n请严格按提示词规定的结构输出第" + stepNo + "步内容。"
                        + "只可使用上述输入中已经确认的事实；示例中的优惠、快递、时效和售后承诺不得直接套用。";
                DeepSeekClient.DeepSeekResult response = deepSeekClient.complete(
                        hookAndClosingPromptService.render(stepNo), userPrompt);
                hookAndClosingPromptService.validateOutput(stepNo, response.content());
                return StepGenerationResult.from(response);
            }
            throw new IllegalArgumentException("当前步骤不支持生成");
        }, executorService);

        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (Exception exception) {
            future.cancel(true);
            log.error("Step {} regeneration timeout or failure after {} seconds",
                    stepNo, timeoutSeconds, exception);
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            if (exception instanceof TimeoutException || cause instanceof TimeoutException) {
                throw new DeepSeekClient.DeepSeekApiException("DeepSeek 响应超时，请稍后重试", cause);
            }
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
            String requestId,
            String model
    ) {
        static StepGenerationResult from(DeepSeekClient.DeepSeekResult response) {
            return new StepGenerationResult(
                    response.content(),
                    response.promptTokens(),
                    response.completionTokens(),
                    response.totalTokens(),
                    response.requestId(),
                    response.model());
        }
    }
}
