package com.linkflow.api.link.controller;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkflow.api.common.exception.ApiExceptionHandler;
import com.linkflow.api.common.web.RequestIdFilter;
import com.linkflow.api.link.dto.LinkSummaryResponse;
import com.linkflow.api.link.service.LinkQueryService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LinkControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private MockMvc createMockMvc(LinkQueryService service) {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        return MockMvcBuilders.standaloneSetup(new LinkController(service))
                .setControllerAdvice(new ApiExceptionHandler())
                .addFilters(new RequestIdFilter())
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void listReturnsEnvelopeAndPageMeta() throws Exception {
        LinkQueryService service = Mockito.mock(LinkQueryService.class);
        MockMvc mockMvc = createMockMvc(service);

        Mockito.when(service.list(1, 20)).thenReturn(new PageImpl<>(
                List.of(new LinkSummaryResponse(
                        UUID.fromString("00000000-0000-0000-0000-000000000007"),
                        "promo2026",
                        "/r/promo2026",
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
                .andExpect(jsonPath("$.data[0].slug").value("promo2026"))
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
                "/r/promo2026",
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
                .andExpect(jsonPath("$.data.short_url").value("/r/promo2026"));
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
}
