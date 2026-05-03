package com.linkflow.api.analytics.service;

import com.linkflow.api.analytics.dto.AccessLogResponse;
import com.linkflow.api.analytics.dto.AnalyticsBreakdownItemResponse;
import com.linkflow.api.analytics.dto.AnalyticsSummaryResponse;
import com.linkflow.api.analytics.dto.AnalyticsTimeseriesPointResponse;
import com.linkflow.api.link.service.LinkQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class LinkAnalyticsService {

    private final LinkQueryService linkQueryService;

    public LinkAnalyticsService(LinkQueryService linkQueryService) {
        this.linkQueryService = linkQueryService;
    }

    /**
     * 汇总单条链接分析数据
     *
     * 当前先校验 link 是否存在；事件存储未接入前返回 0 指标，不伪造数据。
     */
    @Transactional(readOnly = true)
    public AnalyticsSummaryResponse getSummary(UUID linkId, OffsetDateTime from, OffsetDateTime to) {
        linkQueryService.getMappingById(linkId);
        return new AnalyticsSummaryResponse(linkId, 0, 0, 0, from, to);
    }

    /**
     * 查询单条链接趋势
     */
    @Transactional(readOnly = true)
    public List<AnalyticsTimeseriesPointResponse> getTimeseries(
            UUID linkId,
            String granularity,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        linkQueryService.getMappingById(linkId);
        return List.of();
    }

    /**
     * 查询设备分布
     */
    @Transactional(readOnly = true)
    public List<AnalyticsBreakdownItemResponse> getDevices(UUID linkId, OffsetDateTime from, OffsetDateTime to) {
        linkQueryService.getMappingById(linkId);
        return List.of();
    }

    /**
     * 查询浏览器分布
     */
    @Transactional(readOnly = true)
    public List<AnalyticsBreakdownItemResponse> getBrowsers(UUID linkId, OffsetDateTime from, OffsetDateTime to) {
        linkQueryService.getMappingById(linkId);
        return List.of();
    }

    /**
     * 查询地理分布
     */
    @Transactional(readOnly = true)
    public List<AnalyticsBreakdownItemResponse> getLocations(UUID linkId, OffsetDateTime from, OffsetDateTime to) {
        linkQueryService.getMappingById(linkId);
        return List.of();
    }

    /**
     * 查询访问日志
     */
    @Transactional(readOnly = true)
    public List<AccessLogResponse> getAccessLogs(UUID linkId, int page, int size) {
        linkQueryService.getMappingById(linkId);
        return List.of();
    }
}
