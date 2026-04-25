package com.linkflow.api.analytics.service;

import com.linkflow.api.link.domain.UrlMapping;
import com.linkflow.api.link.dto.LinkSummaryResponse;
import com.linkflow.api.link.repository.UrlMappingRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RealtimeAnalyticsServiceTest {

    @Test
    void hotLinksReturnsMappedLinks() {
        UrlMappingRepository repository = Mockito.mock(UrlMappingRepository.class);
        RealtimeAnalyticsService service = new RealtimeAnalyticsService(repository);
        UrlMapping mapping = Mockito.mock(UrlMapping.class);

        Mockito.when(mapping.getId()).thenReturn(3L);
        Mockito.when(mapping.getSlug()).thenReturn("abc1234");
        Mockito.when(mapping.getLongUrl()).thenReturn("https://example.com/landing");
        Mockito.when(mapping.getCreatedAt()).thenReturn(OffsetDateTime.parse("2026-03-01T10:00:00Z"));
        Mockito.when(repository.findAllByOrderByIdDesc(PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(mapping), PageRequest.of(0, 10), 1));

        List<LinkSummaryResponse> result = service.getHotLinks("15m", 10);

        assertEquals(1, result.size());
        assertEquals("/r/abc1234", result.get(0).shortUrl());
    }
}
