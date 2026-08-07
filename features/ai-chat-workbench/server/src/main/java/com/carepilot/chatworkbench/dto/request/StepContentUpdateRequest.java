package com.carepilot.chatworkbench.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request body for manually editing the content of an AI dialog step. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StepContentUpdateRequest {

    @NotBlank(message = "编辑内容不能为空")
    @Size(max = 2000, message = "编辑内容不能超过2000个字符")
    private String content;
}
