package com.carepilot.module.conversation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.carepilot.module.conversation.entity.Conversation;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ConversationMapper extends BaseMapper<Conversation> {
}
