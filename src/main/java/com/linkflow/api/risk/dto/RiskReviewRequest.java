package com.linkflow.api.risk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 风险告警审核请求
 */
public record RiskReviewRequest(
        @NotBlank(message = "action must not be blank")
        @Pattern(regexp = "^(approved|blocked|blacklisted)$", message = "action must be approved, blocked, or blacklisted")
        String action,

        @Size(max = 1000, message = "comment length must be <= 1000")
        String comment
) {
}
