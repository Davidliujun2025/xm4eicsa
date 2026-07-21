package com.carepilot.module.conversation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.carepilot.common.PageResult;
import com.carepilot.common.exception.BusinessException;
import com.carepilot.infra.ai.PromptBuilder;
import com.carepilot.infra.ai.QwenClient;
import com.carepilot.infra.auth.AuthContext;
import com.carepilot.infra.ai.AiGenerationException;
import com.carepilot.module.conversation.dto.*;
import com.carepilot.module.conversation.entity.Conversation;
import com.carepilot.module.conversation.entity.ConversationStep;
import com.carepilot.module.conversation.mapper.ConversationMapper;
import com.carepilot.module.conversation.mapper.ConversationStepMapper;
import com.carepilot.module.conversation.service.ConversationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final ConversationMapper conversationMapper;
    private final ConversationStepMapper stepMapper;
    private final QwenClient qwenClient;
    private final PromptBuilder promptBuilder;
    private final AuthContext authContext;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== 3.1 查询对话历史列表 ====================

    @Override
    public PageResult<ConversationListItem> listConversations(int page, int size, String keyword, String category) {
        Long userId = authContext.getUserId();
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getUserId, userId);

        if (StringUtils.hasText(keyword)) {
            wrapper.like(Conversation::getQuestion, keyword);
        }
        if (StringUtils.hasText(category) && !"全部".equals(category)) {
            wrapper.eq(Conversation::getCategory, category);
        }
        wrapper.orderByDesc(Conversation::getCreatedAt);

        IPage<Conversation> iPage = conversationMapper.selectPage(new Page<>(page, size), wrapper);

        List<ConversationListItem> list = iPage.getRecords().stream()
                .map(c -> ConversationListItem.builder()
                        .id(c.getId())
                        .question(c.getQuestion())
                        .platform(c.getPlatform())
                        .category(c.getCategory())
                        .status(c.getStatus())
                        .createdAt(c.getCreatedAt())
                        .updatedAt(c.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());

        return new PageResult<>(iPage.getTotal(), page, size, list);
    }

    // ==================== 3.2 查询单条对话详情 ====================

    @Override
    public ConversationDetailResponse getConversation(Long id) {
        Conversation conv = conversationMapper.selectById(id);
        if (conv == null) {
            throw new BusinessException(40401, "对话不存在");
        }

        List<ConversationStep> steps = stepMapper.selectList(
                new LambdaQueryWrapper<ConversationStep>()
                        .eq(ConversationStep::getConversationId, id)
                        .orderByAsc(ConversationStep::getStepOrder));

        return buildDetailResponse(conv, steps);
    }

    // ==================== 3.3 创建对话并生成第 1 步 ====================

    @Override
    @Transactional
    public ConversationDetailResponse createConversation(CreateConversationRequest request) {
        Long userId = authContext.getUserId();

        // 1. 插入 conversation
        Conversation conv = new Conversation();
        conv.setUserId(userId);
        conv.setPlatform(request.getPlatform());
        conv.setQuestion(request.getQuestion());
        conv.setStatus("generating");
        conv.setCreatedAt(LocalDateTime.now());
        conv.setUpdatedAt(LocalDateTime.now());
        conversationMapper.insert(conv);

        // 2. 生成第 1 步
        String prompt = promptBuilder.buildStep1Prompt(request.getPlatform(), request.getQuestion());
        String aiResult;
        try {
            aiResult = qwenClient.call(prompt);
        } catch (AiGenerationException e) {
            log.error("AI 生成第1步失败: conversationId={}", conv.getId(), e);
            throw new BusinessException(50001, "AI生成失败，请稍后重试");
        }

        // 3. 解析 + 插入 step1
        ConversationStep step1 = createStep(conv.getId(), userId, 1, aiResult);
        stepMapper.insert(step1);

        // 4. 组装响应
        return buildDetailResponse(conv, List.of(step1));
    }

    // ==================== 3.4 生成下一步骤 ====================

    @Override
    @Transactional
    public ConversationDetailResponse.StepVO generateNextStep(Long conversationId) {
        Conversation conv = conversationMapper.selectById(conversationId);
        if (conv == null) {
            throw new BusinessException(40401, "对话不存在");
        }
        if (!"generating".equals(conv.getStatus())) {
            throw new BusinessException(40003, "当前对话状态不允许继续生成");
        }

        // 查询已有步骤
        List<ConversationStep> existingSteps = stepMapper.selectList(
                new LambdaQueryWrapper<ConversationStep>()
                        .eq(ConversationStep::getConversationId, conversationId)
                        .orderByAsc(ConversationStep::getStepOrder));

        int nextOrder = existingSteps.size() + 1;
        if (nextOrder > 5) {
            throw new BusinessException(40004, "五步法已完整生成，无法继续");
        }

        // 构建 Prompt（注入前序 step 的当前 body）
        String prompt = promptBuilder.buildStepNPrompt(nextOrder, conv.getPlatform(), conv.getQuestion(), existingSteps);
        String aiResult;
        try {
            aiResult = qwenClient.call(prompt);
        } catch (AiGenerationException e) {
            log.error("AI 生成第{}步失败: conversationId={}", nextOrder, conversationId, e);
            throw new BusinessException(50001, "AI生成失败，请稍后重试");
        }

        // 插入新步骤
        ConversationStep newStep = createStep(conversationId, conv.getUserId(), nextOrder, aiResult);
        stepMapper.insert(newStep);

        // 第 5 步完成 → 更新状态
        if (nextOrder == 5) {
            conversationMapper.update(null,
                    new LambdaUpdateWrapper<Conversation>()
                            .eq(Conversation::getId, conversationId)
                            .set(Conversation::getStatus, "success")
                            .set(Conversation::getUpdatedAt, LocalDateTime.now()));
        }

        return toStepVO(newStep);
    }

    // ==================== 3.5 静默保存编辑内容 ====================

    @Override
    @Transactional
    public void saveSteps(Long conversationId, SaveStepsRequest request) {
        Conversation conv = conversationMapper.selectById(conversationId);
        if (conv == null) {
            throw new BusinessException(40401, "对话不存在");
        }

        for (SaveStepsRequest.StepItem item : request.getSteps()) {
            ConversationStep update = new ConversationStep();
            update.setId(item.getStepId());
            update.setBody(item.getBody());
            update.setLabel(item.getLabel());

            if (item.getTags() != null) {
                try {
                    update.setTags(objectMapper.writeValueAsString(item.getTags()));
                } catch (JsonProcessingException e) {
                    log.warn("tags 序列化失败: stepId={}", item.getStepId(), e);
                }
            }

            stepMapper.updateById(update);
        }

        // 同步刷新 conversation.updated_at
        conversationMapper.update(null,
                new LambdaUpdateWrapper<Conversation>()
                        .eq(Conversation::getId, conversationId)
                        .set(Conversation::getUpdatedAt, LocalDateTime.now()));
    }

    // ==================== 私有方法 ====================

    private ConversationStep createStep(Long conversationId, Long userId, int stepOrder, String aiResult) {
        ConversationStep step = new ConversationStep();
        step.setConversationId(conversationId);
        step.setUserId(userId);
        step.setStepOrder(stepOrder);
        step.setStepType(promptBuilder.getStepTypeName(stepOrder));
        step.setTitle(promptBuilder.getStepTitle(stepOrder));
        step.setSection("");  // 框架阶段暂不解析 section
        step.setStatus("");
        step.setCreatedAt(LocalDateTime.now());
        step.setUpdatedAt(LocalDateTime.now());

        // 解析 AI 返回的 JSON
        try {
            JsonNode node = objectMapper.readTree(aiResult);
            step.setBody(node.has("body") ? node.get("body").asText() : "[AI 生成中...]");
            step.setLabel(node.has("label") ? node.get("label").asText() : "待生成");
            step.setTags(node.has("tags") ? node.get("tags").toString() : "[]");
            step.setRiskWarning(node.has("riskWarning") ? node.get("riskWarning").toString() : null);
        } catch (JsonProcessingException e) {
            log.warn("AI 返回 JSON 解析失败，使用占位内容: stepOrder={}", stepOrder, e);
            step.setBody("[AI 生成中...]");
            step.setLabel("待生成");
            step.setTags("[]");
            step.setRiskWarning(null);
        }

        return step;
    }

    private ConversationDetailResponse buildDetailResponse(Conversation conv, List<ConversationStep> steps) {
        List<ConversationDetailResponse.StepVO> stepVOs = steps.stream()
                .map(this::toStepVO)
                .collect(Collectors.toList());

        return ConversationDetailResponse.builder()
                .id(conv.getId())
                .platform(conv.getPlatform())
                .question(conv.getQuestion())
                .category(conv.getCategory())
                .status(conv.getStatus())
                .createdAt(conv.getCreatedAt())
                .updatedAt(conv.getUpdatedAt())
                .steps(stepVOs)
                .tokenInfo(ConversationDetailResponse.TokenInfo.builder()
                        .currentChatUsage(0)
                        .totalLimit(50000)
                        .usedToday(0)
                        .usagePercent(0.0)
                        .build())
                .build();
    }

    private ConversationDetailResponse.StepVO toStepVO(ConversationStep step) {
        List<String> tagsList = Collections.emptyList();
        if (step.getTags() != null) {
            try {
                tagsList = objectMapper.readValue(step.getTags(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
            } catch (JsonProcessingException e) {
                log.warn("tags 反序列化失败: stepId={}", step.getId(), e);
            }
        }

        Object riskWarning = null;
        if (step.getRiskWarning() != null) {
            try {
                riskWarning = objectMapper.readTree(step.getRiskWarning());
            } catch (JsonProcessingException e) {
                log.warn("riskWarning 反序列化失败: stepId={}", step.getId(), e);
            }
        }

        return ConversationDetailResponse.StepVO.builder()
                .stepId(step.getId())
                .stepOrder(step.getStepOrder())
                .stepType(step.getStepType())
                .title(step.getTitle())
                .label(step.getLabel())
                .tags(tagsList)
                .section(step.getSection())
                .body(step.getBody())
                .riskWarning(riskWarning)
                .status(step.getStatus())
                .editable(true)  // 所有步骤始终可编辑
                .build();
    }
}
