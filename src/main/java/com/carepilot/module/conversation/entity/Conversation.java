package com.carepilot.module.conversation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("conversation")
public class Conversation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String platform;
    private String question;
    private String category;
    private String status;

    // 由数据库 DEFAULT CURRENT_TIMESTAMP 处理
    private LocalDateTime createdAt;

    // 由数据库 ON UPDATE CURRENT_TIMESTAMP 处理
    private LocalDateTime updatedAt;
}
