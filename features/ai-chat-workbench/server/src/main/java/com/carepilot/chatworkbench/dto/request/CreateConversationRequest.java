package com.carepilot.chatworkbench.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateConversationRequest {

    @NotBlank(message = "客户问题不能为空")
    @Size(min = 1, max = 500, message = "客户问题长度必须在1-500个字符之间")
    private String question;

    @NotBlank(message = "平台不能为空")
    private String platform;

    private String customerType;
}