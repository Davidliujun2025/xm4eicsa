package com.carepilot.tokenquota.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record RecordTokenUsageRequest(
        @NotBlank(message = "客服账号不能为空")
        String accountNo,

        @NotNull(message = "Token 消耗量不能为空")
        @Positive(message = "Token 消耗量必须为正整数")
        Long consumedTokens,

        @PositiveOrZero(message = "AI 调用次数不能为负数")
        Long aiCallCount,

        @PositiveOrZero(message = "业务数量不能为负数")
        Long businessCount
) {
}
