package com.linkflow.api.analytics.service;

import com.linkflow.api.link.domain.UrlMapping;
import com.linkflow.api.link.dto.response.LinkSummaryResponse;
import com.linkflow.api.link.repository.UrlMappingRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RealtimeAnalyticsServiceTest {

    @Test
    void hotLinksReturnsMappedLinks() {
        UrlMappingRepository repository = Mockito.mock(UrlMappingRepository.class);
        RealtimeAnalyticsService service = new RealtimeAnalyticsService(repository, "http://localhost:8080");
        UrlMapping mapping = Mockito.mock(UrlMapping.class);

        Mockito.when(mapping.getPublicId()).thenReturn(UUID.fromString("018f8f2e-2db8-7a03-8ec0-f4d2fd1a0003"));
        Mockito.when(mapping.getBackHalf()).thenReturn("abc1234");
        Mockito.when(mapping.getLongUrl()).thenReturn("https://example.com/landing");
        Mockito.when(mapping.getCreatedAt()).thenReturn(OffsetDateTime.parse("2026-03-01T10:00:00Z"));
        Mockito.when(repository.findAllByOrderByIdDesc(PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(mapping), PageRequest.of(0, 10), 1));

        List<LinkSummaryResponse> result = service.getHotLinks("15m", 10);

        assertEquals(1, result.size());
        assertEquals("abc1234", result.get(0).backHalf());
        assertEquals("http://localhost:8080/abc1234", result.get(0).shortLink());
    }
}
