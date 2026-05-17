package com.linkflow.api.link.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.linkflow.api.link.domain.BulkLinkItem;

import java.util.UUID;

public record BulkLinkItemResponse(
        UUID id,
        int index,
        String status,
        @JsonProperty("long_url")
        String longUrl,
        @JsonProperty("link_id")
        UUID linkId,
        @JsonProperty("short_url")
        String shortUrl,
        @JsonProperty("risk_level")
        String riskLevel,
        @JsonProperty("risk_score")
        Integer riskScore,
        String category,
        @JsonProperty("error_code")
        String errorCode,
        @JsonProperty("error_message")
        String errorMessage
) {
    public static BulkLinkItemResponse from(BulkLinkItem item, String publicBaseUrl) {
        var link = item.getLink();
        String baseUrl = publicBaseUrl == null ? "" : publicBaseUrl.replaceAll("/+$", "");
        return new BulkLinkItemResponse(
                item.getId(),
                item.getItemIndex(),
                item.getStatus().wireValue(),
                item.getLongUrl(),
                link == null ? null : link.getPublicId(),
                link == null ? null : baseUrl + "/" + link.getBackHalf(),
                item.getRiskLevel(),
                item.getRiskScore(),
                item.getCategory(),
                item.getErrorCode(),
                item.getErrorMessage()
        );
    }
}
