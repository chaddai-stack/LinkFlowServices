package com.linkflow.api.link.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.linkflow.api.link.domain.UrlMapping;

import java.time.OffsetDateTime;
import java.util.UUID;

public record LinkSummaryResponse(
        UUID id,
        String slug,
        @JsonProperty("short_url")
        String shortUrl,
        @JsonProperty("long_url")
        String longUrl,
        String title,
        String channel,
        String status,
        @JsonProperty("click_count")
        long clickCount,
        @JsonProperty("unique_visitors")
        long uniqueVisitors,
        @JsonProperty("created_at")
        OffsetDateTime createdAt,
        @JsonProperty("expires_at")
        OffsetDateTime expiresAt
) {
    public static LinkSummaryResponse from(UrlMapping mapping) {
        return new LinkSummaryResponse(
                toPublicId(mapping.getId()),
                mapping.getSlug(),
                "/r/" + mapping.getSlug(),
                mapping.getLongUrl(),
                null,
                null,
                "active",
                0,
                0,
                mapping.getCreatedAt(),
                null
        );
    }

    public static UUID toPublicId(Long internalId) {
        return new UUID(0L, internalId);
    }
}
