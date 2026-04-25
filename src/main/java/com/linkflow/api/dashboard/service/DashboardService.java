package com.linkflow.api.dashboard.service;

import com.linkflow.api.dashboard.dto.DashboardSummaryResponse;
import com.linkflow.api.link.repository.UrlMappingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class DashboardService {

    private final UrlMappingRepository urlMappingRepository;

    public DashboardService(UrlMappingRepository urlMappingRepository) {
        this.urlMappingRepository = urlMappingRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary(OffsetDateTime from, OffsetDateTime to) {
        long totalLinks = from != null && to != null
                ? urlMappingRepository.countByCreatedAtBetween(from, to)
                : urlMappingRepository.count();

        return new DashboardSummaryResponse(
                totalLinks,
                totalLinks,
                0,
                0
        );
    }
}
