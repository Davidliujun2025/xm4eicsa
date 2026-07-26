package com.example.aics.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public record ToggleFavoriteRequest(
        @Size(max = 128, message = "来源话术ID不能超过128个字符")
        String sourceTalkId,

        @NotBlank(message = "话术完整文本不能为空")
        String content,

        @NotBlank(message = "适用场景不能为空")
        @Size(max = 100, message = "适用场景不能超过100个字符")
        String scenario,

        Instant generatedAt,

        List<@NotBlank(message = "标签不能为空") @Size(max = 5, message = "标签最多5个字符") String> tags
) {
}
