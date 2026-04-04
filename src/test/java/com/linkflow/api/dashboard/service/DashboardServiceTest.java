package com.linkflow.api.dashboard.service;

import com.linkflow.api.dashboard.dto.DashboardSummaryResponse;
import com.linkflow.api.link.repository.UrlMappingRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DashboardServiceTest {

    @Test
    void getSummaryUsesTotalCountWhenRangeMissing() {
        UrlMappingRepository repository = Mockito.mock(UrlMappingRepository.class);
        DashboardService service = new DashboardService(repository);

        Mockito.when(repository.count()).thenReturn(12L);

        DashboardSummaryResponse response = service.getSummary(null, null);

        assertEquals(12L, response.totalLinks());
        assertEquals(12L, response.activeLinks());
        assertEquals(0L, response.totalClicks());
    }

    @Test
    void getSummaryUsesRangeCountWhenRangeProvided() {
        UrlMappingRepository repository = Mockito.mock(UrlMappingRepository.class);
        DashboardService service = new DashboardService(repository);
        OffsetDateTime from = OffsetDateTime.parse("2026-03-01T00:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-03-31T23:59:59Z");

        Mockito.when(repository.countByCreatedAtBetween(from, to)).thenReturn(5L);

        DashboardSummaryResponse response = service.getSummary(from, to);

        assertEquals(5L, response.totalLinks());
        assertEquals(5L, response.activeLinks());
    }
}
