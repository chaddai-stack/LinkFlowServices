package com.linkflow.api.analytics.service;

import com.linkflow.api.link.dto.LinkSummaryResponse;
import com.linkflow.api.link.repository.UrlMappingRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RealtimeAnalyticsService {

    private final UrlMappingRepository urlMappingRepository;

    public RealtimeAnalyticsService(UrlMappingRepository urlMappingRepository) {
        this.urlMappingRepository = urlMappingRepository;
    }

    @Transactional(readOnly = true)
    public List<LinkSummaryResponse> getHotLinks(String window, int limit) {
        return urlMappingRepository.findAllByOrderByIdDesc(PageRequest.of(0, limit))
                .map(LinkSummaryResponse::from)
                .getContent();
    }
}
