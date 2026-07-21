package com.carepilot.module.conversation.service;

import com.carepilot.common.PageResult;
import com.carepilot.module.conversation.dto.*;
import com.carepilot.module.conversation.entity.ConversationStep;

public interface ConversationService {

    /** 查询对话历史列表 */
    PageResult<ConversationListItem> listConversations(int page, int size, String keyword, String category);

    /** 查询单条对话详情 */
    ConversationDetailResponse getConversation(Long id);

    /** 创建对话并生成第 1 步 */
    ConversationDetailResponse createConversation(CreateConversationRequest request);

    /** 生成下一步骤 */
    ConversationDetailResponse.StepVO generateNextStep(Long conversationId);

    /** 静默保存编辑内容 */
    void saveSteps(Long conversationId, SaveStepsRequest request);
}
