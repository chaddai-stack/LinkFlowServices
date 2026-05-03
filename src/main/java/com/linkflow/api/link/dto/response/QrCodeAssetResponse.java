package com.linkflow.api.link.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 二维码云端资产响应。
 *
 * link_id 对外仍是公开 UUID；storage_key/storage_url 表示对象存储中的实际资产位置。
 */
public record QrCodeAssetResponse(
        UUID id,
        @JsonProperty("link_id")
        UUID linkId,
        String format,
        int size,
        @JsonProperty("storage_key")
        String storageKey,
        @JsonProperty("storage_url")
        String storageUrl,
        @JsonProperty("created_at")
        OffsetDateTime createdAt
) {
}
