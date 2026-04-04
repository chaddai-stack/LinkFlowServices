package com.linkflow.api.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DashboardSummaryResponse(
        @JsonProperty("total_links")
        long totalLinks,
        @JsonProperty("active_links")
        long activeLinks,
        @JsonProperty("total_clicks")
        long totalClicks,
        @JsonProperty("unique_visitors")
        long uniqueVisitors
) {
}
