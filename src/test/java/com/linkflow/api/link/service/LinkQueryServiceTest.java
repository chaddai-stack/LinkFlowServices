package com.linkflow.api.link.service;

import com.linkflow.api.common.error.ApiException;
import com.linkflow.api.link.domain.UrlMapping;
import com.linkflow.api.link.dto.LinkSummaryResponse;
import com.linkflow.api.link.repository.UrlMappingRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LinkQueryServiceTest {

    @Test
    void listReturnsMappedPage() {
        UrlMappingRepository repository = Mockito.mock(UrlMappingRepository.class);
        LinkQueryService service = new LinkQueryService(repository);
        UrlMapping mapping = Mockito.mock(UrlMapping.class);

        Mockito.when(mapping.getId()).thenReturn(7L);
        Mockito.when(mapping.getSlug()).thenReturn("promo2026");
        Mockito.when(mapping.getLongUrl()).thenReturn("https://example.com/campaign");
        Mockito.when(mapping.getCreatedAt()).thenReturn(OffsetDateTime.parse("2026-03-01T10:00:00Z"));
        Mockito.when(repository.findAllByOrderByIdDesc(PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(mapping), PageRequest.of(0, 20), 1));

        LinkSummaryResponse item = service.list(1, 20).getContent().get(0);

        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000007"), item.id());
        assertEquals("promo2026", item.slug());
        assertEquals("/r/promo2026", item.shortUrl());
    }

    @Test
    void getByIdThrowsLinkNotFoundWhenMissing() {
        UrlMappingRepository repository = Mockito.mock(UrlMappingRepository.class);
        LinkQueryService service = new LinkQueryService(repository);
        UUID linkId = UUID.fromString("00000000-0000-0000-0000-000000000063");

        Mockito.when(repository.findById(99L)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class, () -> service.getById(linkId));

        assertEquals("LINK_NOT_FOUND", exception.getCode());
    }
}
