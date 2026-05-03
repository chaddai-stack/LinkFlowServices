package com.linkflow.api.analytics.dto;

/**
 * 分析维度拆分项
 */
public record AnalyticsBreakdownItemResponse(
        String name,
        long clicks,
        double percentage
) {
}
