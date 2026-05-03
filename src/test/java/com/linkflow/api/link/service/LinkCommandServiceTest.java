package com.linkflow.api.link.service;

import com.linkflow.api.link.domain.UrlMapping;
import com.linkflow.api.link.dto.request.CreateLinkRequest;
import com.linkflow.api.link.repository.UrlMappingRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;

class LinkCommandServiceTest {

    @Test
    void createReusesExistingLinkWhenRandomBackHalfWouldBeGenerated() {
        UrlMappingRepository repository = Mockito.mock(UrlMappingRepository.class);
        LinkTitleCrawlerService titleCrawlerService = Mockito.mock(LinkTitleCrawlerService.class);
        LinkCommandService service = new LinkCommandService(repository, titleCrawlerService);
        UrlMapping existing = new UrlMapping("https://example.com/campaign", "abc1234");
        CreateLinkRequest request = new CreateLinkRequest(
                "https://example.com/campaign",
                "Campaign",
                null,
                null,
                null,
                List.of(),
                Map.of()
        );

        Mockito.when(repository.findFirstByLongUrlOrderByIdAsc("https://example.com/campaign"))
                .thenReturn(Optional.of(existing));
        Mockito.when(titleCrawlerService.resolveTitle("https://example.com/campaign", "Campaign"))
                .thenReturn("Campaign");

        UrlMapping result = service.create(request);

        assertSame(existing, result);
        Mockito.verify(repository, Mockito.never()).save(Mockito.any(UrlMapping.class));
    }

    @Test
    void createWithCustomBackHalfDoesNotReuseExistingLongUrl() {
        UrlMappingRepository repository = Mockito.mock(UrlMappingRepository.class);
        LinkTitleCrawlerService titleCrawlerService = Mockito.mock(LinkTitleCrawlerService.class);
        LinkCommandService service = new LinkCommandService(repository, titleCrawlerService);
        CreateLinkRequest request = new CreateLinkRequest(
                "https://example.com/campaign",
                "Campaign",
                "promo2026",
                null,
                null,
                List.of(),
                Map.of()
        );
        UrlMapping saved = new UrlMapping("https://example.com/campaign", "promo2026");

        Mockito.when(repository.existsByBackHalf("promo2026")).thenReturn(false);
        Mockito.when(repository.save(Mockito.any(UrlMapping.class))).thenReturn(saved);
        Mockito.when(titleCrawlerService.resolveTitle("https://example.com/campaign", "Campaign"))
                .thenReturn("Campaign");

        UrlMapping result = service.create(request);

        assertSame(saved, result);
        Mockito.verify(repository, Mockito.never()).findFirstByLongUrlOrderByIdAsc(Mockito.anyString());
    }
}
