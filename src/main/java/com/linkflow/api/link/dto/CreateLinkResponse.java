package com.linkflow.api.link.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

public record CreateLinkResponse(
        String slug,
        @JsonProperty("short_url")
        String shortUrl,
        @JsonProperty("long_url")
        String longUrl,
        @JsonProperty("created_at")
        OffsetDateTime createdAt
) {
}
