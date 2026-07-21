package com.carepilot.chatworkbench.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenInfo {

    private Integer currentChatUsage;
    private Integer totalLimit;
    private Integer usedToday;
    private Double usagePercent;
}