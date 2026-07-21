package com.carepilot.chatworkbench.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteScriptRequest {

    @NotBlank(message = "话术内容不能为空")
    private String scriptContent;

    private String conversationId;

    @NotBlank(message = "平台不能为空")
    private String platform;

    private String customerType;

    @NotBlank(message = "话术步骤不能为空")
    private String scriptStep;

    private String question;
}