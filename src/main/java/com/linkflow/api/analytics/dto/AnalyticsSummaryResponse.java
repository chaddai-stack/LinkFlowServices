package com.linkflow.api.analytics.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 单链接分析汇总
 */
public record AnalyticsSummaryResponse(
        @JsonProperty("link_id")
        UUID linkId,
        long clicks,
        @JsonProperty("unique_visitors")
        long uniqueVisitors,
        @JsonProperty("bot_clicks")
        long botClicks,
        @JsonProperty("from")
        OffsetDateTime from,
        @JsonProperty("to")
        OffsetDateTime to
) {
}
