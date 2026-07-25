package com.carepilot.tokenquota.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record BatchUpdateDailyQuotaRequest(
        @NotEmpty(message = "请选择需要调整的客服账号")
        List<@NotBlank(message = "客服账号不能为空") String> accountNos,

        @NotNull(message = "每日 Token 配额不能为空")
        @Positive(message = "每日 Token 配额必须为正整数")
        Long dailyQuota,

        @NotBlank(message = "调整原因不能为空")
        String reason,

        @NotBlank(message = "操作人 ID 不能为空")
        String operatorId,

        @NotBlank(message = "操作人姓名不能为空")
        String operatorName
) {
}
