package com.linkflow.api.link.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateLinkStatusRequest(
        @NotBlank(message = "status is required")
        @Pattern(regexp = "^(active|paused|expired|blocked)$", message = "status must be one of active, paused, expired, blocked")
        String status
) {
}
