package com.carepilot.module.conversation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class SaveStepsRequest {

    @NotEmpty(message = "步骤列表不能为空")
    @Valid
    private List<StepItem> steps;

    @Data
    public static class StepItem {
        @NotNull(message = "步骤ID不能为空")
        private Long stepId;

        @NotNull(message = "步骤序号不能为空")
        private Integer stepOrder;

        @NotBlank(message = "步骤内容不能为空")
        @Size(max = 65535, message = "步骤内容过长")
        private String body;

        @Size(max = 32, message = "标签不能超过32字符")
        private String label;

        @Size(max = 10, message = "标签最多10个")
        private List<String> tags;
    }
}
