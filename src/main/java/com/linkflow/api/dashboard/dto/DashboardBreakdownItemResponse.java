package com.linkflow.api.dashboard.dto;

/**
 * 仪表盘分组统计项
 */
public record DashboardBreakdownItemResponse(
        String name,
        long links,
        long clicks
) {
}
