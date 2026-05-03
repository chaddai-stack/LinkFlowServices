package com.linkflow.api.link.controller;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkflow.api.common.exception.ApiExceptionHandler;
import com.linkflow.api.common.web.RequestIdFilter;
import com.linkflow.api.link.domain.LinkStatus;
import com.linkflow.api.link.domain.UrlMapping;
import com.linkflow.api.link.dto.request.CreateLinkRequest;
import com.linkflow.api.link.dto.response.LinkSummaryResponse;
import com.linkflow.api.link.dto.request.UpdateLinkRequest;
import com.linkflow.api.link.dto.request.UpdateLinkStatusRequest;
import com.linkflow.api.link.service.BackHalfRecommendationService;
import com.linkflow.api.link.service.LinkCommandService;
import com.linkflow.api.link.service.LinkQueryService;
import com.linkflow.api.link.service.LinkTitleCrawlerService;
import com.linkflow.api.link.service.QrCodeAssetService;
import com.linkflow.api.link.service.QrCodeService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LinkControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private MockMvc createMockMvc(LinkQueryService queryService) {
        return createMockMvc(queryService, Mockito.mock(LinkCommandService.class), Mockito.mock(QrCodeAssetService.class));
    }

    private MockMvc createMockMvc(
            LinkQueryService queryService,
            LinkCommandService commandService,
            QrCodeAssetService qrCodeAssetService
    ) {
        return createMockMvc(queryService, commandService, qrCodeAssetService, Mockito.mock(QrCodeService.class));
    }

    private MockMvc createMockMvc(
            LinkQueryService queryService,
            LinkCommandService commandService,
            QrCodeAssetService qrCodeAssetService,
            QrCodeService qrCodeService
    ) {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        return MockMvcBuilders.standaloneSetup(new LinkController(
                        queryService,
                        commandService,
                        Mockito.mock(LinkTitleCrawlerService.class),
                        Mockito.mock(BackHalfRecommendationService.class),
                        qrCodeAssetService,
                        qrCodeService,
                        "http://localhost:8080"
                ))
                .setControllerAdvice(new ApiExceptionHandler())
                .addFilters(new RequestIdFilter())
                .setValidator(validator)
                .setMessageConverters(
                        new ByteArrayHttpMessageConverter(),
                        new MappingJackson2HttpMessageConverter(objectMapper)
                )
                .build();
    }

    @Test
    void createReturnsEnvelope() throws Exception {
        LinkQueryService queryService = Mockito.mock(LinkQueryService.class);
        LinkCommandService commandService = Mockito.mock(LinkCommandService.class);
        MockMvc mockMvc = createMockMvc(queryService, commandService, Mockito.mock(QrCodeAssetService.class));

        UrlMapping mapping = mapping(7L, "promo2026", "https://example.com/campaign", "Spring campaign", "wechat", LinkStatus.ACTIVE);
        Mockito.when(commandService.create(Mockito.any(CreateLinkRequest.class))).thenReturn(mapping);

        mockMvc.perform(post("/api/v1/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "long_url": "https://example.com/campaign",
                                  "title": "Spring campaign",
                                  "custom_back_half": "promo2026",
                                  "channel": "wechat"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.data.back_half").value("promo2026"))
                .andExpect(jsonPath("$.data.short_link").value("http://localhost:8080/promo2026"))
                .andExpect(jsonPath("$.data.title").value("Spring campaign"))
                .andExpect(jsonPath("$.data.channel").value("wechat"))
                .andExpect(jsonPath("$.data.status").value("active"))
                .andExpect(jsonPath("$.error").value(nullValue()));
    }

    @Test
    void listReturnsEnvelopeAndPageMeta() throws Exception {
        LinkQueryService service = Mockito.mock(LinkQueryService.class);
        MockMvc mockMvc = createMockMvc(service);

        Mockito.when(service.list(1, 20, null, null, "created_at,desc")).thenReturn(new PageImpl<>(
                List.of(new LinkSummaryResponse(
                        UUID.fromString("00000000-0000-0000-0000-000000000007"),
                        "promo2026",
                        "http://localhost:8080/promo2026",
                        "https://example.com/campaign",
                        null,
                        null,
                        "active",
                        0,
                        0,
                        OffsetDateTime.parse("2026-03-01T10:00:00Z"),
                        null
                )),
                PageRequest.of(0, 20),
                1
        ));

        mockMvc.perform(get("/api/v1/links"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.data[0].back_half").value("promo2026"))
                .andExpect(jsonPath("$.data[0].short_link").value("http://localhost:8080/promo2026"))
                .andExpect(jsonPath("$.meta.page.page").value(1))
                .andExpect(jsonPath("$.error").value(nullValue()));
    }

    @Test
    void detailReturnsEnvelope() throws Exception {
        LinkQueryService service = Mockito.mock(LinkQueryService.class);
        MockMvc mockMvc = createMockMvc(service);
        UUID linkId = UUID.fromString("00000000-0000-0000-0000-000000000007");

        Mockito.when(service.getById(linkId)).thenReturn(new LinkSummaryResponse(
                linkId,
                "promo2026",
                "http://localhost:8080/promo2026",
                "https://example.com/campaign",
                null,
                null,
                "active",
                0,
                0,
                OffsetDateTime.parse("2026-03-01T10:00:00Z"),
                null
        ));

        mockMvc.perform(get("/api/v1/links/{linkId}", linkId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(linkId.toString()))
                .andExpect(jsonPath("$.data.link_id").value(linkId.toString()))
                .andExpect(jsonPath("$.data.back_half").value("promo2026"))
                .andExpect(jsonPath("$.data.short_link").value("http://localhost:8080/promo2026"));
    }

    @Test
    void listPassesFilterAndSortParams() throws Exception {
        LinkQueryService service = Mockito.mock(LinkQueryService.class);
        MockMvc mockMvc = createMockMvc(service);

        Mockito.when(service.list(1, 20, "active", "promo", "created_at,desc"))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/api/v1/links")
                        .param("status", "active")
                        .param("search", "promo")
                        .param("sort", "created_at,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.page.total_elements").value(0));
    }

    @Test
    void updateReturnsEnvelope() throws Exception {
        LinkQueryService queryService = Mockito.mock(LinkQueryService.class);
        LinkCommandService commandService = Mockito.mock(LinkCommandService.class);
        MockMvc mockMvc = createMockMvc(queryService, commandService, Mockito.mock(QrCodeAssetService.class));
        UUID linkId = UUID.fromString("00000000-0000-0000-0000-000000000007");
        UrlMapping updated = mapping(7L, "new-slug", "https://example.com/new", "New title", "email", LinkStatus.ACTIVE);

        Mockito.when(commandService.update(eq(linkId), Mockito.any(UpdateLinkRequest.class)))
                .thenReturn(updated);

        mockMvc.perform(patch("/api/v1/links/{linkId}", linkId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "long_url": "https://example.com/new",
                                  "title": "New title",
                                  "custom_back_half": "new-slug",
                                  "channel": "email"
                                }
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.back_half").value("new-slug"))
                .andExpect(jsonPath("$.data.short_link").value("http://localhost:8080/new-slug"))
                .andExpect(jsonPath("$.data.title").value("New title"))
                .andExpect(jsonPath("$.data.channel").value("email"));
    }

    @Test
    void updateStatusReturnsEnvelope() throws Exception {
        LinkQueryService queryService = Mockito.mock(LinkQueryService.class);
        LinkCommandService commandService = Mockito.mock(LinkCommandService.class);
        MockMvc mockMvc = createMockMvc(queryService, commandService, Mockito.mock(QrCodeAssetService.class));
        UUID linkId = UUID.fromString("00000000-0000-0000-0000-000000000007");
        UrlMapping paused = mapping(7L, "promo2026", "https://example.com/campaign", "Spring campaign", "wechat", LinkStatus.PAUSED);

        Mockito.when(commandService.updateStatus(eq(linkId), Mockito.any(UpdateLinkStatusRequest.class)))
                .thenReturn(paused);

        mockMvc.perform(patch("/api/v1/links/{linkId}/status", linkId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "paused"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("paused"));
    }

    @Test
    void deleteReturnsEnvelope() throws Exception {
        LinkQueryService queryService = Mockito.mock(LinkQueryService.class);
        LinkCommandService commandService = Mockito.mock(LinkCommandService.class);
        MockMvc mockMvc = createMockMvc(queryService, commandService, Mockito.mock(QrCodeAssetService.class));
        UUID linkId = UUID.fromString("00000000-0000-0000-0000-000000000007");

        mockMvc.perform(delete("/api/v1/links/{linkId}", linkId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error").value(nullValue()));

        Mockito.verify(commandService).delete(linkId);
    }

    @Test
    void qrCodeReturnsPng() throws Exception {
        LinkQueryService queryService = Mockito.mock(LinkQueryService.class);
        QrCodeAssetService qrCodeAssetService = Mockito.mock(QrCodeAssetService.class);
        QrCodeService qrCodeService = Mockito.mock(QrCodeService.class);
        MockMvc mockMvc = createMockMvc(queryService, Mockito.mock(LinkCommandService.class), qrCodeAssetService, qrCodeService);
        UUID linkId = UUID.fromString("00000000-0000-0000-0000-000000000007");
        byte[] png = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47};
        UrlMapping mapping = mapping(7L, "promo2026", "https://example.com/campaign", "Spring campaign", "wechat", LinkStatus.ACTIVE);

        Mockito.when(queryService.getMappingById(linkId))
                .thenReturn(mapping);
        Mockito.when(qrCodeService.generatePng("http://localhost:8080/promo2026", 256))
                .thenReturn(png);

        mockMvc.perform(get("/api/v1/links/{linkId}/qrcode", linkId)
                        .param("size", "256"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(png));
        Mockito.verifyNoInteractions(qrCodeAssetService);
    }

    @Test
    void listRejectsSizeAboveLimit() throws Exception {
        LinkQueryService service = Mockito.mock(LinkQueryService.class);
        MockMvc mockMvc = createMockMvc(service);

        mockMvc.perform(get("/api/v1/links").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details.size").exists());
    }

    private UrlMapping mapping(Long id, String backHalf, String longUrl, String title, String channel, LinkStatus status) {
        UrlMapping mapping = Mockito.mock(UrlMapping.class);
        Mockito.when(mapping.getPublicId()).thenReturn(new UUID(0x018f8f2e2db87a03L, id));
        Mockito.when(mapping.getBackHalf()).thenReturn(backHalf);
        Mockito.when(mapping.getLongUrl()).thenReturn(longUrl);
        Mockito.when(mapping.getTitle()).thenReturn(title);
        Mockito.when(mapping.getChannel()).thenReturn(channel);
        Mockito.when(mapping.getStatus()).thenReturn(status);
        Mockito.when(mapping.getCreatedAt()).thenReturn(OffsetDateTime.parse("2026-03-01T10:00:00Z"));
        return mapping;
    }
}
