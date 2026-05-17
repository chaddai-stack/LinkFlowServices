package com.linkflow.api.link.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BulkCreateLinksRequest(
        @Size(max = 40, message = "mode length must be <= 40")
        String mode,

        @NotNull(message = "items must be provided")
        @Size(min = 1, max = 100, message = "items size must be between 1 and 100")
        List<BulkCreateLinkItemRequest> items
) {
    public record BulkCreateLinkItemRequest(
            @JsonProperty("long_url")
            String longUrl,

            String title,

            @JsonProperty("custom_back_half")
            String customBackHalf,

            String channel,

            @JsonProperty("expires_at")
            java.time.OffsetDateTime expiresAt
    ) {
    }
}
