package com.linkflow.api.analytics.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 实时分析概览
 */
public record RealtimeOverviewResponse(
        String window,
        @JsonProperty("active_links")
        long activeLinks,
        @JsonProperty("clicks")
        long clicks,
        @JsonProperty("unique_visitors")
        long uniqueVisitors,
        @JsonProperty("events_per_second")
        double eventsPerSecond
) {
}
