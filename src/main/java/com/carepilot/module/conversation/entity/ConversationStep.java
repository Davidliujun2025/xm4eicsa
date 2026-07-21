package com.carepilot.module.conversation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("conversation_step")
public class ConversationStep {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long conversationId;
    private Long userId;
    private Integer stepOrder;
    private String stepType;
    private String title;
    private String label;
    private String tags;      // JSON 字符串，Java 层用 String 接收
    private String section;
    private String body;
    private String riskWarning; // JSON 字符串，仅 step3 有值
    private String status;

    // 由数据库 DEFAULT CURRENT_TIMESTAMP 处理
    private LocalDateTime createdAt;

    // 由数据库 ON UPDATE CURRENT_TIMESTAMP 处理
    private LocalDateTime updatedAt;
}
