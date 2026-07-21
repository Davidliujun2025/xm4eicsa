package com.carepilot.module.conversation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateConversationRequest {

    @NotBlank(message = "平台不能为空")
    private String platform;

    @NotBlank(message = "客户问题不能为空")
    @Size(max = 500, message = "客户问题不能超过500字符")
    private String question;
}
