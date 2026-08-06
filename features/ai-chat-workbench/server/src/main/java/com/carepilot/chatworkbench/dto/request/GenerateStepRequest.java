package com.carepilot.chatworkbench.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Optional input for generating an AI dialog step.  Step two uses carrierName
 * when the prior intent indicates a logistics concern.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateStepRequest {

    @Size(max = 30, message = "合作快递名称不能超过30个字符")
    private String carrierName;
}
