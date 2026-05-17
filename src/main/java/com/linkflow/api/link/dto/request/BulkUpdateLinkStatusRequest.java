package com.linkflow.api.link.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * 批量更新已创建链接状态的请求。
 */
public record BulkUpdateLinkStatusRequest(
        @JsonProperty("link_ids")
        @NotEmpty(message = "link_ids must not be empty")
        @Size(max = 100, message = "link_ids size must be <= 100")
        List<@NotNull(message = "link_id must not be null") UUID> linkIds,

        @NotBlank(message = "status is required")
        @Pattern(regexp = "^(active|paused|expired|blocked)$", message = "status must be one of active, paused, expired, blocked")
        String status
) {
}
