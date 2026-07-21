package com.carepilot.infra.ai;

import com.carepilot.module.conversation.entity.ConversationStep;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 串行 Prompt 组装器。
 * 根据当前步骤序号和已有上下文动态组装 Prompt 字符串。
 */
@Component
public class PromptBuilder {

    public String buildStep1Prompt(String platform, String question) {
        return buildBasePrompt(platform, question);
    }

    public String buildStepNPrompt(int stepOrder, String platform, String question,
                                   List<ConversationStep> previousSteps) {
        StringBuilder sb = new StringBuilder(buildBasePrompt(platform, question));
        sb.append("\n\n【前序步骤内容】\n");
        previousSteps.forEach(step -> {
            sb.append("\n").append(step.getTitle()).append(":\n");
            sb.append(step.getBody()).append("\n");
        });

        if (stepOrder == 3) {
            sb.append("\n【风险检测要求】\n");
            sb.append("请检测推荐话术是否符合平台规则，如有风险请标注。\n");
        }

        return sb.toString();
    }

    private String buildBasePrompt(String platform, String question) {
        return "你是一个专业的电商客服助手，正在为用户提供客服话术支持。\n"
                + "服务平台：" + platform + "\n"
                + "客户问题：" + question + "\n";
    }

    public String getStepTypeName(int stepOrder) {
        return switch (stepOrder) {
            case 1 -> "意图识别";
            case 2 -> "回复策略";
            case 3 -> "推荐话术";
            case 4 -> "钩子引导";
            case 5 -> "成功收尾";
            default -> throw new IllegalArgumentException("步骤序号超出1-5范围: " + stepOrder);
        };
    }

    public String getStepTitle(int stepOrder) {
        return "第" + stepOrder + "步 - " + getStepTypeName(stepOrder);
    }
}
