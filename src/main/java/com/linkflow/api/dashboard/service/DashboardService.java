package com.linkflow.api.dashboard.service;

import com.linkflow.api.dashboard.dto.DashboardBreakdownItemResponse;
import com.linkflow.api.dashboard.dto.DashboardSummaryResponse;
import com.linkflow.api.dashboard.dto.DashboardTrendPointResponse;
import com.linkflow.api.link.domain.LinkStatus;
import com.linkflow.api.link.repository.UrlMappingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class DashboardService {

    private final UrlMappingRepository urlMappingRepository;

    public DashboardService(UrlMappingRepository urlMappingRepository) {
        this.urlMappingRepository = urlMappingRepository;
    }

    /**
     * 汇总链接维度指标
     */
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary(OffsetDateTime from, OffsetDateTime to) {
        long totalLinks = from != null && to != null
                ? urlMappingRepository.countByCreatedAtBetween(from, to)
                : urlMappingRepository.count();
        long activeLinks = urlMappingRepository.countByStatus(LinkStatus.ACTIVE);

        return new DashboardSummaryResponse(
                totalLinks,
                activeLinks,
                0,
                0
        );
    }

    /**
     * 趋势序列
     *
     * 当前没有事件流水表，因此返回空集合；后续接入事件存储后在这里替换聚合查询。
     */
    @Transactional(readOnly = true)
    public List<DashboardTrendPointResponse> getTrends(String granularity, OffsetDateTime from, OffsetDateTime to) {
        return List.of();
    }

    /**
     * 渠道维度统计
     */
    @Transactional(readOnly = true)
    public List<DashboardBreakdownItemResponse> getChannels(OffsetDateTime from, OffsetDateTime to) {
        return urlMappingRepository.countByChannel().stream()
                .map(row -> new DashboardBreakdownItemResponse((String) row[0], (Long) row[1], 0))
                .toList();
    }

    /**
     * 地理维度统计
     */
    @Transactional(readOnly = true)
    public List<DashboardBreakdownItemResponse> getLocations(OffsetDateTime from, OffsetDateTime to) {
        return List.of();
    }
}
