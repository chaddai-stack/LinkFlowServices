package com.linkflow.api.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

/**
 * 仪表盘趋势点
 */
public record DashboardTrendPointResponse(
        @JsonProperty("bucket_start")
        OffsetDateTime bucketStart,
        @JsonProperty("links_created")
        long linksCreated,
        long clicks,
        @JsonProperty("unique_visitors")
        long uniqueVisitors
) {
}
