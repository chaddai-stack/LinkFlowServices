package com.linkflow.api.link.service;

import com.linkflow.api.common.error.ApiException;
import com.linkflow.api.link.domain.UrlMapping;
import com.linkflow.api.link.dto.response.LinkSummaryResponse;
import com.linkflow.api.link.repository.UrlMappingRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

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
        LinkQueryService service = new LinkQueryService(repository, "http://localhost:8080");
        UrlMapping mapping = Mockito.mock(UrlMapping.class);
        UUID publicId = UUID.fromString("018f8f2e-2db8-7a03-8ec0-f4d2fd1a0007");

        Mockito.when(mapping.getPublicId()).thenReturn(publicId);
        Mockito.when(mapping.getBackHalf()).thenReturn("promo2026");
        Mockito.when(mapping.getLongUrl()).thenReturn("https://example.com/campaign");
        Mockito.when(mapping.getCreatedAt()).thenReturn(OffsetDateTime.parse("2026-03-01T10:00:00Z"));
        Mockito.when(repository.findAll(Mockito.<Specification<UrlMapping>>any(), Mockito.eq(PageRequest.of(0, 20, org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC,
                        "createdAt"
                )))))
                .thenReturn(new PageImpl<>(List.of(mapping), PageRequest.of(0, 20), 1));

        LinkSummaryResponse item = service.list(1, 20, "active", "promo", "created_at,desc").getContent().get(0);

        assertEquals(publicId, item.id());
        assertEquals("promo2026", item.backHalf());
        assertEquals("promo2026", item.backHalf());
        assertEquals("http://localhost:8080/promo2026", item.shortLink());
    }

    @Test
    void getByIdThrowsLinkNotFoundWhenMissing() {
        UrlMappingRepository repository = Mockito.mock(UrlMappingRepository.class);
        LinkQueryService service = new LinkQueryService(repository, "http://localhost:8080");
        UUID linkId = UUID.fromString("018f8f2e-2db8-7a03-8ec0-f4d2fd1a0063");

        Mockito.when(repository.findByPublicId(linkId)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class, () -> service.getById(linkId));

        assertEquals("LINK_NOT_FOUND", exception.getCode());
    }
}
