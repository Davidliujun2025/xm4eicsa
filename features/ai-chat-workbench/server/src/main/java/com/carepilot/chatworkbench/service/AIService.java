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
    private final ReplyStrategyRuleService replyStrategyRuleService;
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
                ? resolveRememberedCarrier(conversation.getId(), current)
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

    /**
     * 重新生成第2步时找回合作快递：优先取当前生效记录，其次回退到该会话
     * 历史轮次/历史版本第2步记录中保存过的合作快递元数据。
     */
    private String resolveRememberedCarrier(Long sessionTaskId, AiDialogStepRecord current) {
        String remembered = replyStrategyRuleService.carrierFromMetadata(current.getExtraJson());
        if (remembered != null) {
            return remembered;
        }
        List<AiDialogStepRecord> stepTwoRecords = stepRecordRepository
                .findBySessionTaskIdAndStepNoAndIsDeleteOrderByDialogRoundDescStepRoundDesc(
                        sessionTaskId, (byte) 2, (byte) 0);
        for (AiDialogStepRecord candidate : stepTwoRecords) {
            String carrier = replyStrategyRuleService.carrierFromMetadata(candidate.getExtraJson());
            if (carrier != null) {
                return carrier;
            }
        }
        return null;
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
                                                     String previousContent, String carrierName) {
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
                String intentRecognition = historyRecords.stream()
                        .filter(record -> dialogRound.equals(record.getDialogRound()))
                        .filter(record -> record.getStepNo() != null && record.getStepNo().intValue() == 1)
                        .map(AiDialogStepRecord::getAiContent)
                        .findFirst().orElse("");
                validateIntentSufficiency(question, intentRecognition);
                ReplyStrategyRuleService.ReplyStrategyPlan plan = replyStrategyRuleService.plan(
                        platform, question, intentRecognition, carrierName);
                String userPrompt = "服务平台：" + platform + "\n\n客户当前问题：" + question
                        + "\n\n意图识别及上下文：\n" + generationContext
                        + "\n\n请按提示词规定的“回复策略、回复理由、预期效果、对转化的影响”四部分输出第2步正文。";
                DeepSeekClient.DeepSeekResult response = deepSeekClient.complete(plan.prompt(), userPrompt);
                ReplyStrategyRuleService.ValidationResult validation = replyStrategyRuleService.validate(response.content(), plan);
                for (int attempt = 0; attempt < 2 && !validation.valid(); attempt++) {
                    String retryPrompt = plan.prompt() + "\n上次输出未通过校验："
                            + validation.message() + "。请严格修正后重新输出。";
                    if (validation.message().contains("模糊快递")) {
                        retryPrompt += "\n不要出现或否定任何模糊快递词，直接写“我们发" + plan.carrierName() + "”。";
                    }
                    response = deepSeekClient.complete(retryPrompt, userPrompt);
                    validation = replyStrategyRuleService.validate(response.content(), plan);
                }
                if (!validation.valid()) {
                    throw new IllegalStateException("回复策略未通过业务校验：" + validation.message());
                }
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

    private void validateIntentSufficiency(String customerQuestion, String intentRecognition) {
        String question = safe(customerQuestion).trim();
        String intent = safe(intentRecognition).trim();
        if (intent.isBlank()) {
            throw new IllegalStateException("请先完成意图识别");
        }
        // 意图识别模板约定：五个维度有效意图少于3个时，结论中必须明确输出“有效意图少于3个，当前信息不足”。
        // 只认这个显式标记，避免正常意图中维度级的“无法识别/尚无法确认/当前信息不足以判断…”被误判为信息不足。
        boolean sparseIntent = intent.contains("有效意图少于3个");
        boolean bareGreeting = question.length() <= 6
                && question.matches(".*(在吗|你好|您好|hello|hi|在不在|有人吗|有人么).*");
        // AC8：客户已表达具体商品/功能/价格/库存/场景等询问时，即使有效意图少于3个，
        // 也应按“热情破冰+开放式场景提问”生成策略，不按 AC13 拦截。
        boolean concreteInquiry = hasConcreteInquiry(question);
        // 客户亲口表达物流顾虑时优先按 AC3 走物流策略；其余信息不足场景按 AC13 拦截
        if ((sparseIntent || bareGreeting) && !hasLogisticsMarker(question) && !concreteInquiry) {
            throw new IllegalStateException("当前客户信息不足，无法生成精准回复策略，建议先返回第一步补充澄清引导");
        }
    }

    private boolean hasConcreteInquiry(String value) {
        if (value == null) {
            return false;
        }
        return java.util.regex.Pattern
                .compile("商品|产品|这款|哪款|哪种|这个|那个|有货|还有吗|多少钱|价格|便宜|贵不贵|贵吗|划算|性价比|优惠|包邮|运费|折扣|怎么用|怎么操作|怎么样|如何|适合|适配|好不好|好用|耐用|质量|材质|尺寸|颜色|大小|型号|款式|容量|能不能|可不可以|是否|功能|效果|送人|送礼|生日|节日|自用|场景|下单|购买|拍下|退货|退款|退换|换货|售后|保修|正品|真假|对比|区别")
                .matcher(value)
                .find();
    }

    private boolean hasLogisticsMarker(String value) {
        return value != null && java.util.regex.Pattern
                .compile("快递|物流|配送|发货|派送|送货|运送")
                .matcher(value)
                .find();
    }

    private String safe(String value) { return value == null ? "" : value; }

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
