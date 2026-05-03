package com.linkflow.api.link.dto.response;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.linkflow.api.link.domain.LinkStatus;
import com.linkflow.api.link.domain.UrlMapping;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 短链摘要响应。
 */
public record LinkSummaryResponse(
        UUID id,
        @JsonProperty("back_half")
        String backHalf,
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
    @JsonGetter("link_id")
    public UUID linkId() {
        return id;
    }

    @JsonGetter("short_link")
    public String shortLink() {
        return shortUrl;
    }

    /**
     * 将持久化短链转换为 API 响应。
     */
    public static LinkSummaryResponse from(UrlMapping mapping) {
        return from(mapping, "");
    }

    /**
     * 将持久化短链转换为带完整短链地址的 API 响应。
     */
    public static LinkSummaryResponse from(UrlMapping mapping, String publicBaseUrl) {
        String baseUrl = publicBaseUrl == null ? "" : publicBaseUrl.replaceAll("/+$", "");
        String shortLink = baseUrl.isBlank() ? "/" + mapping.getBackHalf() : baseUrl + "/" + mapping.getBackHalf();
        return new LinkSummaryResponse(
                mapping.getPublicId(),
                mapping.getBackHalf(),
                shortLink,
                mapping.getLongUrl(),
                mapping.getTitle(),
                mapping.getChannel(),
                mapping.getStatus() == null ? LinkStatus.ACTIVE.wireValue() : mapping.getStatus().wireValue(),
                0,
                0,
                mapping.getCreatedAt(),
                mapping.getExpiresAt()
        );
    }
}
