package com.carepilot.module.conversation.controller;

import com.carepilot.common.PageResult;
import com.carepilot.common.Result;
import com.carepilot.module.conversation.dto.*;
import com.carepilot.module.conversation.service.ConversationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    /** 3.1 查询对话历史列表 */
    @GetMapping
    public Result<PageResult<ConversationListItem>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category) {
        return Result.ok(conversationService.listConversations(page, size, keyword, category));
    }

    /** 3.2 查询单条对话详情 */
    @GetMapping("/{id}")
    public Result<ConversationDetailResponse> get(@PathVariable Long id) {
        return Result.ok(conversationService.getConversation(id));
    }

    /** 3.3 创建对话并生成第 1 步 */
    @PostMapping
    public Result<ConversationDetailResponse> create(@Valid @RequestBody CreateConversationRequest request) {
        return Result.ok(conversationService.createConversation(request));
    }

    /** 3.4 生成下一步骤 */
    @PostMapping("/{id}/generate-next")
    public Result<ConversationDetailResponse.StepVO> generateNext(@PathVariable Long id) {
        return Result.ok(conversationService.generateNextStep(id));
    }

    /** 3.5 静默保存编辑内容 */
    @PutMapping("/{id}")
    public Result<Void> save(@PathVariable Long id, @Valid @RequestBody SaveStepsRequest request) {
        conversationService.saveSteps(id, request);
        return Result.ok();
    }
}
