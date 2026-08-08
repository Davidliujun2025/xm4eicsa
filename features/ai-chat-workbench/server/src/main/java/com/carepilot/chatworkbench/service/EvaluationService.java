package com.carepilot.chatworkbench.service;

import com.carepilot.chatworkbench.dto.response.EvaluationPageResponse;
import com.carepilot.chatworkbench.dto.response.EvaluationStatsResponse;
import com.carepilot.chatworkbench.entity.AiDialogStepRecord;
import com.carepilot.chatworkbench.entity.Conversation;
import com.carepilot.chatworkbench.entity.CustomerServiceEvaluationReport;
import com.carepilot.chatworkbench.repository.AiDialogStepRecordRepository;
import com.carepilot.chatworkbench.repository.ConversationRepository;
import com.carepilot.chatworkbench.repository.CustomerServiceEvaluationReportRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class EvaluationService {

    private final ConversationRepository conversationRepository;
    private final AiDialogStepRecordRepository stepRecordRepository;
    private final CustomerServiceEvaluationReportRepository reportRepository;
    private final DeepSeekClient deepSeekClient;
    private final TokenService tokenService;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final Map<String, Integer> weights;
    private final String evaluationPrompt;

    public EvaluationService(
            ConversationRepository conversationRepository,
            AiDialogStepRecordRepository stepRecordRepository,
            CustomerServiceEvaluationReportRepository reportRepository,
            DeepSeekClient deepSeekClient,
            TokenService tokenService,
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            @Value("${app.evaluation.weights.service-attitude:20}") int serviceAttitudeWeight,
            @Value("${app.evaluation.weights.problem-solving:25}") int problemSolvingWeight,
            @Value("${app.evaluation.weights.empathy:15}") int empathyWeight,
            @Value("${app.evaluation.weights.compliance:20}") int complianceWeight,
            @Value("${app.evaluation.weights.conversion-guidance:15}") int conversionGuidanceWeight,
            @Value("${app.evaluation.weights.response-efficiency:5}") int responseEfficiencyWeight
    ) {
        this.conversationRepository = conversationRepository;
        this.stepRecordRepository = stepRecordRepository;
        this.reportRepository = reportRepository;
        this.deepSeekClient = deepSeekClient;
        this.tokenService = tokenService;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.weights = normalizedWeights(serviceAttitudeWeight, problemSolvingWeight, empathyWeight,
                complianceWeight, conversionGuidanceWeight, responseEfficiencyWeight);
        try {
            this.evaluationPrompt = new ClassPathResource("prompts/evaluation-report.txt")
                    .getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException("无法加载客服评估提示词", exception);
        }
    }

    @Transactional
    public CustomerServiceEvaluationReport generateForCompletedConversation(
            String operatorId, String conversationId) {
        CustomerServiceEvaluationReport existing = reportRepository.findByConversationId(conversationId)
                .orElse(null);
        if (existing != null) {
            if (!operatorId.equals(existing.getOperatorId())) {
                throw new SecurityException("无权访问该评估报告");
            }
            return existing;
        }

        Conversation conversation = ownedConversation(operatorId, conversationId);
        List<AiDialogStepRecord> records = stepRecordRepository
                .findBySessionTaskIdAndIsEffectiveAndIsDeleteOrderByDialogRoundAscStepNoAsc(
                        conversation.getId(), (byte) 1, (byte) 0);
        boolean hasClosing = records.stream().anyMatch(record -> record.getStepNo() != null
                && record.getStepNo() == 5 && record.getAiContent() != null
                && !record.getAiContent().isBlank());
        if (!hasClosing) {
            throw new IllegalStateException("请先完成第五步成功收尾，再结束并生成评估报告");
        }

        String transcript = buildTranscript(records);
        ForbiddenHits forbiddenHits = scanForbiddenWords(conversation.getPlatform(), transcript);
        long averageResponseMillis = averageResponseMillis(records);
        String userPrompt = "服务平台：" + conversation.getPlatform()
                + "\n评估权重（总分由系统计算）：" + toJson(weights)
                + "\n平均模型响应时长（毫秒）：" + averageResponseMillis
                + "\n违禁词命中次数：" + forbiddenHits.count()
                + "\n违禁词：" + forbiddenHits.words()
                + "\n\n完整对话记录：\n" + truncate(transcript, 30000);
        DeepSeekClient.DeepSeekResult aiResult = deepSeekClient.complete(evaluationPrompt, userPrompt);
        JsonNode result = parseResult(aiResult.content());

        int serviceAttitude = score(result, "serviceAttitude");
        int problemSolving = score(result, "problemSolving");
        int empathy = score(result, "empathy");
        int compliance = score(result, "compliance");
        int conversionGuidance = score(result, "conversionGuidance");
        int responseEfficiency = score(result, "responseEfficiency");
        int totalScore = weightedTotal(serviceAttitude, problemSolving, empathy, compliance,
                conversionGuidance, responseEfficiency);

        CustomerServiceEvaluationReport report = reportRepository.save(
                CustomerServiceEvaluationReport.builder()
                        .conversationId(conversationId)
                        .sessionTaskId(conversation.getId())
                        .operatorId(operatorId)
                        .platform(conversation.getPlatform())
                        .totalScore(totalScore)
                        .serviceAttitudeScore(serviceAttitude)
                        .problemSolvingScore(problemSolving)
                        .empathyScore(empathy)
                        .complianceScore(compliance)
                        .conversionGuidanceScore(conversionGuidance)
                        .responseEfficiencyScore(responseEfficiency)
                        .dimensionWeightsJson(toJson(weights))
                        .strengths(text(result, "strengths", "未提供"))
                        .problems(text(result, "problems", "未发现明显问题"))
                        .suggestions(text(result, "suggestions", "保持专业、准确、克制的服务表达"))
                        .forbiddenHitCount(forbiddenHits.count())
                        .forbiddenWordsJson(toJson(forbiddenHits.words()))
                        .transcript(transcript)
                        .totalTokens(tokenService.getSessionUsedTokens(conversation.getId()))
                        .build());
        tokenService.recordEvaluationUsage(operatorId, conversationId, aiResult);
        return report;
    }

    public EvaluationPageResponse searchMine(String operatorId, LocalDate from, LocalDate to,
                                               Integer minScore, Integer maxScore, String platform,
                                               int page, int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(100, size));
        Page<CustomerServiceEvaluationReport> result = reportRepository.searchMine(
                operatorId,
                from == null ? null : from.atStartOfDay(),
                to == null ? null : to.plusDays(1).atStartOfDay(),
                normalizeScore(minScore), normalizeScore(maxScore),
                platform == null ? null : platform.trim(),
                PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "generatedAt")));
        return new EvaluationPageResponse(result.getContent(), result.getTotalElements(), safePage,
                safeSize, result.getTotalPages());
    }

    public CustomerServiceEvaluationReport getMine(String operatorId, Long reportId) {
        return reportRepository.findByIdAndOperatorId(reportId, operatorId)
                .orElseThrow(() -> new IllegalArgumentException("评估报告不存在"));
    }

    public EvaluationStatsResponse statsMine(String operatorId) {
        int used = tokenService.getTodayUsedTokens(operatorId);
        int limit = tokenService.getDailyLimit(operatorId);
        return new EvaluationStatsResponse(
                used, Math.max(0, limit - used), limit,
                reportRepository.countMine(operatorId),
                Math.round(reportRepository.averageMine(operatorId) * 10.0) / 10.0,
                tokenService.getCumulativeUsedTokens(operatorId));
    }

    public List<CustomerServiceEvaluationReport> allMine(String operatorId, LocalDate from,
                                                          LocalDate to, Integer minScore,
                                                          Integer maxScore, String platform) {
        List<CustomerServiceEvaluationReport> all = new ArrayList<>();
        int currentPage = 1;
        EvaluationPageResponse result;
        do {
            result = searchMine(operatorId, from, to, minScore, maxScore, platform, currentPage, 100);
            all.addAll(result.items());
            currentPage++;
        } while (currentPage <= result.totalPages());
        return all;
    }

    private Conversation ownedConversation(String operatorId, String conversationId) {
        Conversation conversation = conversationRepository.findByConversationId(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation does not exist"));
        if (!operatorId.equals(conversation.getCustomerId())) {
            throw new SecurityException("无权访问该会话");
        }
        return conversation;
    }

    private String buildTranscript(List<AiDialogStepRecord> records) {
        StringBuilder transcript = new StringBuilder();
        Integer currentRound = null;
        for (AiDialogStepRecord record : records) {
            if (!record.getDialogRound().equals(currentRound)) {
                currentRound = record.getDialogRound();
                transcript.append("\n第").append(currentRound).append("轮客户问题：")
                        .append(record.getCustomerDialog()).append("\n");
            }
            transcript.append("第").append(record.getStepNo()).append("步AI输出：")
                    .append(record.getAiContent() == null ? "" : record.getAiContent()).append("\n");
        }
        return transcript.toString().trim();
    }

    private ForbiddenHits scanForbiddenWords(String platform, String transcript) {
        try {
            List<String> words = jdbcTemplate.queryForList(
                    "SELECT word FROM forbidden_word WHERE platform IN ('ALL', ?)",
                    String.class, platformKey(platform));
            List<String> hits = new ArrayList<>();
            int count = 0;
            for (String word : words) {
                int from = 0;
                boolean hit = false;
                while (word != null && !word.isBlank()
                        && (from = transcript.indexOf(word, from)) >= 0) {
                    count++;
                    hit = true;
                    from += word.length();
                }
                if (hit) hits.add(word);
            }
            return new ForbiddenHits(count, hits.stream().distinct().toList());
        } catch (RuntimeException ignored) {
            return new ForbiddenHits(0, List.of());
        }
    }

    private String platformKey(String platform) {
        if (platform == null) return "OTHER";
        String value = platform.trim().toUpperCase(Locale.ROOT);
        return switch (value) {
            case "淘宝" -> "TAOBAO";
            case "天猫" -> "TMALL";
            case "京东" -> "JD";
            case "拼多多" -> "PINDUODUO";
            case "抖音" -> "DOUYIN";
            case "小红书" -> "XIAOHONGSHU";
            case "快手" -> "KUAISHOU";
            case "视频号" -> "SHIPINHAO";
            case "微信小店" -> "WECHAT_SHOP";
            default -> value.matches("[A-Z_]+") ? value : "OTHER";
        };
    }

    private JsonNode parseResult(String content) {
        String normalized = content == null ? "" : content.trim();
        if (normalized.startsWith("```")) {
            normalized = normalized.replaceFirst("^```(?:json)?\\s*", "")
                    .replaceFirst("\\s*```$", "");
        }
        try {
            JsonNode node = objectMapper.readTree(normalized);
            if (!node.isObject()) throw new IllegalArgumentException("评估结果不是JSON对象");
            return node;
        } catch (Exception exception) {
            throw new DeepSeekClient.DeepSeekApiException("DeepSeek 返回的评估报告格式无效", exception);
        }
    }

    private int score(JsonNode node, String field) {
        if (!node.has(field) || !node.get(field).canConvertToInt()) {
            throw new DeepSeekClient.DeepSeekApiException("评估报告缺少分项得分：" + field);
        }
        return Math.max(0, Math.min(100, node.get(field).asInt()));
    }

    private String text(JsonNode node, String field, String fallback) {
        String value = node.path(field).asText("").trim();
        return value.isBlank() ? fallback : value;
    }

    private int weightedTotal(int... scores) {
        String[] keys = {"serviceAttitude", "problemSolving", "empathy", "compliance",
                "conversionGuidance", "responseEfficiency"};
        double result = 0;
        for (int i = 0; i < keys.length; i++) result += scores[i] * weights.get(keys[i]) / 100.0;
        return (int) Math.round(result);
    }

    private Map<String, Integer> normalizedWeights(int... raw) {
        String[] keys = {"serviceAttitude", "problemSolving", "empathy", "compliance",
                "conversionGuidance", "responseEfficiency"};
        int sum = 0;
        for (int value : raw) sum += Math.max(0, value);
        if (sum <= 0) throw new IllegalArgumentException("评估维度权重总和必须大于0");
        Map<String, Integer> result = new LinkedHashMap<>();
        int allocated = 0;
        for (int i = 0; i < raw.length; i++) {
            int weight = (int) Math.floor(Math.max(0, raw[i]) * 100.0 / sum);
            result.put(keys[i], weight);
            allocated += weight;
        }
        int remainder = 100 - allocated;
        for (int i = 0; i < remainder; i++) {
            String key = keys[i % keys.length];
            result.put(key, result.get(key) + 1);
        }
        return result;
    }

    private long averageResponseMillis(List<AiDialogStepRecord> records) {
        return Math.round(records.stream()
                .filter(r -> r.getTriggerAt() != null && r.getLlmReplyAt() != null)
                .mapToLong(r -> Math.max(0, java.time.Duration.between(r.getTriggerAt(), r.getLlmReplyAt()).toMillis()))
                .average().orElse(0));
    }

    private Integer normalizeScore(Integer score) {
        return score == null ? null : Math.max(0, Math.min(100, score));
    }

    private String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(value.length() - max);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("无法序列化评估数据", exception);
        }
    }

    private record ForbiddenHits(int count, List<String> words) { }
}
