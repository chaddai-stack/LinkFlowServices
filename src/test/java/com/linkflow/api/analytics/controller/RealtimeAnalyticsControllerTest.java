package com.linkflow.api.analytics.controller;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkflow.api.analytics.service.RealtimeAnalyticsService;
import com.linkflow.api.common.exception.ApiExceptionHandler;
import com.linkflow.api.common.web.RequestIdFilter;
import com.linkflow.api.link.dto.LinkSummaryResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.OffsetDateTime;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RealtimeAnalyticsControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private MockMvc createMockMvc(RealtimeAnalyticsService service) {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        return MockMvcBuilders.standaloneSetup(new RealtimeAnalyticsController(service))
                .setControllerAdvice(new ApiExceptionHandler())
                .addFilters(new RequestIdFilter())
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void hotLinksReturnsEnvelope() throws Exception {
        RealtimeAnalyticsService service = Mockito.mock(RealtimeAnalyticsService.class);
        MockMvc mockMvc = createMockMvc(service);

        Mockito.when(service.getHotLinks("15m", 10)).thenReturn(List.of(new LinkSummaryResponse(
                LinkSummaryResponse.toPublicId(3L),
                "abc1234",
                "/r/abc1234",
                "https://example.com/landing",
                null,
                null,
                "active",
                0,
                0,
                OffsetDateTime.parse("2026-03-01T10:00:00Z"),
                null
        )));

        mockMvc.perform(get("/api/v1/analytics/realtime/hot-links"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.data[0].slug").value("abc1234"))
                .andExpect(jsonPath("$.meta.window").value("15m"))
                .andExpect(jsonPath("$.error").value(nullValue()));
    }
}
