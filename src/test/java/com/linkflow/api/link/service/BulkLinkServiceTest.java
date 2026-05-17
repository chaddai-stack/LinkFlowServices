package com.linkflow.api.link.service;

import com.linkflow.api.link.domain.BulkLinkItem;
import com.linkflow.api.link.domain.BulkLinkJob;
import com.linkflow.api.link.domain.UrlMapping;
import com.linkflow.api.link.dto.request.BulkCreateLinksRequest;
import com.linkflow.api.link.dto.response.LinkClassificationResponse;
import com.linkflow.api.link.repository.BulkLinkItemRepository;
import com.linkflow.api.link.repository.BulkLinkJobRepository;
import com.linkflow.api.risk.dto.RiskScanTaskResponse;
import com.linkflow.api.risk.service.RiskService;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class BulkLinkServiceTest {

    @Test
    void createKeepsItemFailuresInsideBulkResult() {
        BulkLinkJobRepository jobRepository = Mockito.mock(BulkLinkJobRepository.class);
        BulkLinkItemRepository itemRepository = Mockito.mock(BulkLinkItemRepository.class);
        LinkCommandService linkCommandService = Mockito.mock(LinkCommandService.class);
        RiskService riskService = Mockito.mock(RiskService.class);
        LinkClassificationService classificationService = Mockito.mock(LinkClassificationService.class);
        BulkLinkService service = new BulkLinkService(
                jobRepository,
                itemRepository,
                linkCommandService,
                riskService,
                classificationService,
                Validation.buildDefaultValidatorFactory().getValidator(),
                "http://localhost:8080"
        );
        UrlMapping link = Mockito.mock(UrlMapping.class);
        UUID linkId = UUID.fromString("00000000-0000-0000-0000-000000000011");
        when(link.getPublicId()).thenReturn(linkId);
        when(link.getBackHalf()).thenReturn("valid");
        when(itemRepository.save(any(BulkLinkItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(linkCommandService.create(any())).thenReturn(link);
        when(riskService.createScanTask(any())).thenReturn(new RiskScanTaskResponse(
                UUID.randomUUID(),
                linkId,
                "https://example.com",
                "succeeded",
                "low",
                0,
                List.of("basic_url_checks_passed"),
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                null
        ));
        when(classificationService.classify(linkId)).thenReturn(new LinkClassificationResponse(
                linkId,
                "developer",
                0.85,
                List.of("rule_keyword_match"),
                "rules",
                OffsetDateTime.now(),
                OffsetDateTime.now()
        ));

        final BulkLinkJob[] savedJob = new BulkLinkJob[1];
        when(jobRepository.findById(any(UUID.class))).thenAnswer(invocation -> Optional.of(savedJob[0]));
        when(jobRepository.save(any(BulkLinkJob.class))).thenAnswer(invocation -> {
            savedJob[0] = invocation.getArgument(0);
            if (savedJob[0].getId() == null) {
                ReflectionTestUtils.setField(savedJob[0], "id", UUID.fromString("00000000-0000-0000-0000-000000000099"));
            }
            return savedJob[0];
        });
        when(itemRepository.findByJobOrderByItemIndexAsc(any(BulkLinkJob.class))).thenAnswer(invocation -> List.of());

        var response = service.create(new BulkCreateLinksRequest(
                "create_scan_classify",
                List.of(
                        new BulkCreateLinksRequest.BulkCreateLinkItemRequest("https://example.com", "Valid", null, "test", null),
                        new BulkCreateLinksRequest.BulkCreateLinkItemRequest("not-a-url", "Invalid", null, "test", null)
                )
        ));

        assertEquals("partial_failed", response.status());
        assertEquals(1, response.succeeded());
        assertEquals(1, response.failed());
    }
}
