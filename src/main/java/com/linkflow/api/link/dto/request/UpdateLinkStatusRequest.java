package com.linkflow.api.link.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 短链状态更新请求
 */
public record UpdateLinkStatusRequest(
        @NotBlank(message = "status is required")
        @Pattern(regexp = "^(active|paused|expired|blocked)$", message = "status must be one of active, paused, expired, blocked")
        String status
) {
}
