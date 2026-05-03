package com.linkflow.api.analytics.service;

import com.linkflow.api.analytics.dto.RealtimeOverviewResponse;
import com.linkflow.api.link.domain.LinkStatus;
import com.linkflow.api.link.dto.response.LinkSummaryResponse;
import com.linkflow.api.link.repository.UrlMappingRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RealtimeAnalyticsService {

    private final UrlMappingRepository urlMappingRepository;
    private final String publicBaseUrl;

    public RealtimeAnalyticsService(
            UrlMappingRepository urlMappingRepository,
            @Value("${app.public-base-url:http://localhost:8080}") String publicBaseUrl
    ) {
        this.urlMappingRepository = urlMappingRepository;
        this.publicBaseUrl = publicBaseUrl;
    }

    /**
     * 查询热门链接
     *
     * 在事件聚合表接入前，返回最近创建的链接，保证前端契约稳定且不伪造点击数。
     */
    @Transactional(readOnly = true)
    public List<LinkSummaryResponse> getHotLinks(String window, int limit) {
        return urlMappingRepository.findAllByOrderByIdDesc(PageRequest.of(0, limit))
                .map(mapping -> LinkSummaryResponse.from(mapping, publicBaseUrl))
                .getContent();
    }

    /**
     * 查询实时概览
     */
    @Transactional(readOnly = true)
    public RealtimeOverviewResponse getOverview(String window) {
        return new RealtimeOverviewResponse(
                window,
                urlMappingRepository.countByStatus(LinkStatus.ACTIVE),
                0,
                0,
                0.0
        );
    }
}
