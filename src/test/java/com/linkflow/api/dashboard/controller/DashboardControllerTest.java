package com.linkflow.api.dashboard.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkflow.api.common.exception.ApiExceptionHandler;
import com.linkflow.api.common.web.RequestIdFilter;
import com.linkflow.api.dashboard.dto.DashboardSummaryResponse;
import com.linkflow.api.dashboard.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DashboardControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc createMockMvc(DashboardService service) {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        return MockMvcBuilders.standaloneSetup(new DashboardController(service))
                .setControllerAdvice(new ApiExceptionHandler())
                .addFilters(new RequestIdFilter())
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void summaryReturnsEnvelope() throws Exception {
        DashboardService service = Mockito.mock(DashboardService.class);
        MockMvc mockMvc = createMockMvc(service);

        Mockito.when(service.getSummary(null, null)).thenReturn(new DashboardSummaryResponse(12, 12, 0, 0));

        mockMvc.perform(get("/api/v1/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.data.total_links").value(12))
                .andExpect(jsonPath("$.error").value(nullValue()));
    }

    @Test
    void summaryRejectsPartialRange() throws Exception {
        DashboardService service = Mockito.mock(DashboardService.class);
        MockMvc mockMvc = createMockMvc(service);

        mockMvc.perform(get("/api/v1/dashboard/summary")
                        .param("from", "2026-03-01T00:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details.from").value("from and to must be provided together."))
                .andExpect(jsonPath("$.error.details.to").value("from and to must be provided together."));
    }
}
