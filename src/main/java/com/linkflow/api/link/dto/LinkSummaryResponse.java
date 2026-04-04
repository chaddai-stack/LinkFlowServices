package com.linkflow.api.link.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.linkflow.api.link.domain.UrlMapping;

import java.time.OffsetDateTime;

public record LinkSummaryResponse(
        Long id,
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
                mapping.getId(),
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
}
