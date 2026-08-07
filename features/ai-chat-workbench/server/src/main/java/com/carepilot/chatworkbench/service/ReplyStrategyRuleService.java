package com.carepilot.chatworkbench.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Business rules for step two of the five-step customer-service flow. */
@Service
public class ReplyStrategyRuleService {

    private static final Pattern LOGISTICS_MARKER = Pattern.compile("快递|物流|配送|发货|派送|送货|运送");
    private static final Pattern EMPATHY_MARKER = Pattern.compile("理解|确实|担心|不放心|抱歉|辛苦|顾虑");
    private static final Pattern DAMAGE_MARKER = Pattern.compile("破损.{0,8}(包赔|赔付)|(包赔|赔付).{0,8}破损");
    private static final Pattern LOST_MARKER = Pattern.compile("丢件.{0,8}(补发|赔付)|(补发|赔付).{0,8}丢件");
    private static final Pattern TRACKING_MARKER = Pattern.compile("(全程)?(物流)?(跟踪|追踪)|(异常).{0,8}(处理|跟进)");
    private static final Pattern URGENCY_MARKER = Pattern.compile("(今天|现在|马上|立即|尽快|尽早|当天|今日|此刻|这就).{0,12}(下单|拍下|发出|发货|出库|安排)|下单.{0,12}(发货|出库|发出|安排)");
    private static final Pattern FORBIDDEN_DEFENCE = Pattern.compile("快递.{0,4}(挺好|很好|没问题)|不用担心.{0,8}(快递|物流)");
    private static final Pattern VAGUE_CARRIER = Pattern.compile("(?<!不是)(?<!并非)(?<!不会)(?<!没有)(?<!不采用)(随机快递|仓库安排|默认快递|任意快递)");

    public ReplyStrategyPlan plan(String platform, String customerQuestion, String intentRecognition,
                                  String carrierName) {
        boolean logisticsConcern = hasLogisticsConcern(customerQuestion, intentRecognition);
        String normalizedCarrier = normalizeCarrierName(carrierName);
        if (logisticsConcern && normalizedCarrier == null) {
            throw new IllegalArgumentException("检测到客户存在物流顾虑，请先填写具体合作快递名称");
        }

        String platformGuidance = platformGuidance(platform);
        if (logisticsConcern) {
            String prompt = "你正在生成五步生成法第2步“回复策略”。客户存在物流/快递顾虑。"
                    + "只输出简明、可执行的策略正文，不输出Markdown标题。必须严格按以下顺序组织："
                    + "1) 先共情并站在客户角度认可其顾虑；"
                    + "2) 明确说明本次合作快递为“" + normalizedCarrier + "”；"
                    + "3) 明确给出破损包赔、丢件补发、全程物流跟踪并在异常时处理三项保障；"
                    + "4) 结尾必须使用明确的时效推进话术引导客户下单（如“今天下单今天发”“现在下单，今天就能发出”“尽快发出”等），"
                    + "不得仅陈述事实，必须包含下单/发货的时间紧迫感。"
                    + "禁止反驳、辩护或美化当前快递服务。直接写“我们发" + normalizedCarrier + "”即可，"
                    + "不要出现“随机快递”“仓库安排”“默认快递”“任意快递”等字样，也不要写“不是随机快递”这类否定句。"
                    + "平台适配要求：" + platformGuidance;
            return new ReplyStrategyPlan(true, normalizedCarrier, prompt);
        }

        String prompt = "你正在生成五步生成法第2步“回复策略”。客户没有物流/快递顾虑。"
                + "只输出简明、可执行的策略正文，不输出Markdown标题。采用“热情破冰 + 不催促"
                + " + 场景/设备提问 + 降低决策门槛”的策略，不得输出物流安抚、快递承诺或发货承诺。"
                + "优先用1至2个回答成本低的问题引导客户补充使用场景。平台适配要求：" + platformGuidance;
        return new ReplyStrategyPlan(false, null, prompt);
    }

    public boolean hasLogisticsConcern(String customerQuestion, String intentRecognition) {
        // 仅以客户原话判断物流顾虑，避免意图识别文本中的“发货/物流”等泛化措辞（如引导问题）误触发
        return LOGISTICS_MARKER.matcher(safe(customerQuestion)).find();
    }

    public ValidationResult validate(String content, ReplyStrategyPlan plan) {
        if (!plan.logisticsConcern()) {
            boolean containsLogisticsPromise = LOGISTICS_MARKER.matcher(safe(content)).find();
            return containsLogisticsPromise
                    ? ValidationResult.invalid(List.of("非物流场景不得输出物流安抚或快递承诺"))
                    : ValidationResult.passed();
        }

        String value = safe(content);
        List<String> violations = new ArrayList<>();
        if (!EMPATHY_MARKER.matcher(value).find()) violations.add("缺少共情站队表达");
        if (!value.contains(plan.carrierName())) violations.add("缺少具体合作快递名称");
        if (!DAMAGE_MARKER.matcher(value).find()) violations.add("缺少破损包赔承诺");
        if (!LOST_MARKER.matcher(value).find()) violations.add("缺少丢件补发承诺");
        if (!TRACKING_MARKER.matcher(value).find()) violations.add("缺少全程物流跟踪承诺");
        if (!URGENCY_MARKER.matcher(value).find()) violations.add("缺少时效下单推进钩子");
        if (FORBIDDEN_DEFENCE.matcher(value).find()) violations.add("出现了美化或辩护快递的禁用表达");
        if (VAGUE_CARRIER.matcher(value).find()) violations.add("出现了模糊快递表述");

        int empathy = firstMatch(EMPATHY_MARKER, value);
        int carrier = value.indexOf(plan.carrierName());
        int guarantee = minPositive(firstMatch(DAMAGE_MARKER, value), firstMatch(LOST_MARKER, value),
                firstMatch(TRACKING_MARKER, value));
        int urgency = firstMatch(URGENCY_MARKER, value);
        if (empathy >= 0 && carrier >= 0 && guarantee >= 0 && urgency >= 0
                && !(empathy < carrier && carrier < guarantee && guarantee < urgency)) {
            violations.add("四要素顺序必须为共情站队→快递名称→保障承诺→时效推进");
        }
        return violations.isEmpty() ? ValidationResult.passed() : ValidationResult.invalid(violations);
    }

    public String carrierMetadata(String carrierName) {
        String normalized = normalizeCarrierName(carrierName);
        return normalized == null ? null : "{\"carrierName\":\"" + normalized.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}";
    }

    public String carrierFromMetadata(String extraJson) {
        if (extraJson == null) return null;
        java.util.regex.Matcher matcher = Pattern.compile("\\\"carrierName\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").matcher(extraJson);
        return matcher.find() ? normalizeCarrierName(matcher.group(1)) : null;
    }

    private String normalizeCarrierName(String carrierName) {
        if (carrierName == null || carrierName.trim().isEmpty()) return null;
        String normalized = carrierName.trim();
        if (VAGUE_CARRIER.matcher(normalized).find()) {
            throw new IllegalArgumentException("请填写具体合作快递名称，不能使用随机快递或仓库安排");
        }
        return normalized;
    }

    private String platformGuidance(String platform) {
        String value = safe(platform).toUpperCase(Locale.ROOT);
        if (value.contains("淘宝") || value.contains("天猫") || value.contains("TAOBAO") || value.contains("TMALL")) {
            return "突出破损包赔；如有已确认的店铺规则可提及正品保障，禁止编造。";
        }
        if (value.contains("京东") || value.contains("JD")) return "侧重物流时效；仅在事实明确时提及自营配送。";
        if (value.contains("拼多多") || value.contains("PINDUODUO")) return "侧重包邮、实惠、省心；禁止虚构优惠。";
        if (value.contains("抖音") || value.contains("快手") || value.contains("DOUYIN") || value.contains("KUAISHOU")) {
            return "侧重发货速度和已确认的活动福利；禁止虚构活动。";
        }
        return "按通用电商场景表达，并提示选择具体平台可提升策略精准度。";
    }

    private int firstMatch(Pattern pattern, String value) {
        java.util.regex.Matcher matcher = pattern.matcher(value);
        return matcher.find() ? matcher.start() : -1;
    }

    private int minPositive(int... values) {
        int result = Integer.MAX_VALUE;
        for (int value : values) if (value >= 0) result = Math.min(result, value);
        return result == Integer.MAX_VALUE ? -1 : result;
    }

    private String safe(String value) { return value == null ? "" : value; }

    public record ReplyStrategyPlan(boolean logisticsConcern, String carrierName, String prompt) { }
    public record ValidationResult(boolean valid, List<String> violations) {
        static ValidationResult passed() { return new ValidationResult(true, List.of()); }
        static ValidationResult invalid(List<String> violations) { return new ValidationResult(false, List.copyOf(violations)); }
        String message() { return String.join("；", violations); }
    }
}
