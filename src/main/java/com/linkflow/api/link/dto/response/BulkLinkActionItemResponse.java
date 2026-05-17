package com.linkflow.api.link.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * 单条链接批量操作结果。
 */
public record BulkLinkActionItemResponse(
        @JsonProperty("link_id")
        UUID linkId,
        String status,
        @JsonProperty("error_code")
        String errorCode,
        @JsonProperty("error_message")
        String errorMessage
) {
    public static BulkLinkActionItemResponse succeeded(UUID linkId) {
        return new BulkLinkActionItemResponse(linkId, "succeeded", null, null);
    }

    public static BulkLinkActionItemResponse failed(UUID linkId, String errorCode, String errorMessage) {
        return new BulkLinkActionItemResponse(linkId, "failed", errorCode, errorMessage);
    }
}
