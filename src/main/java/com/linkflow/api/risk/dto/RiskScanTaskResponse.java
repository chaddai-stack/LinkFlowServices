package com.linkflow.api.risk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 风险扫描任务响应
 */
public record RiskScanTaskResponse(
        @JsonProperty("task_id")
        UUID taskId,
        @JsonProperty("link_id")
        UUID linkId,
        @JsonProperty("long_url")
        String longUrl,
        String status,
        @JsonProperty("created_at")
        OffsetDateTime createdAt
) {
}
