package com.linkflow.api.analytics.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

/**
 * 单链接分析趋势点
 */
public record AnalyticsTimeseriesPointResponse(
        @JsonProperty("bucket_start")
        OffsetDateTime bucketStart,
        long clicks,
        @JsonProperty("unique_visitors")
        long uniqueVisitors
) {
}
