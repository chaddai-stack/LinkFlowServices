package com.linkflow.api.link.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkflow.api.common.exception.ApiExceptionHandler;
import com.linkflow.api.common.web.RequestIdFilter;
import com.linkflow.api.link.domain.UrlMapping;
import com.linkflow.api.link.dto.CreateLinkRequest;
import com.linkflow.api.link.dto.CreateShortUrlRequest;
import com.linkflow.api.link.service.UrlCodecService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UrlControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc createMockMvc(UrlCodecService urlCodecService) {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        return MockMvcBuilders.standaloneSetup(new UrlController(urlCodecService))
                .setControllerAdvice(new ApiExceptionHandler())
                .addFilters(new RequestIdFilter())
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void createReturnsShortUrlPayload() throws Exception {
        UrlCodecService urlCodecService = Mockito.mock(UrlCodecService.class);
        MockMvc mockMvc = createMockMvc(urlCodecService);

        Mockito.when(urlCodecService.createOrGet("https://example.com/landing"))
                .thenReturn(new UrlMapping("https://example.com/landing", "abc1234"));

        mockMvc.perform(post("/api/short-urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateShortUrlRequest("https://example.com/landing"))))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.slug").value("abc1234"))
                .andExpect(jsonPath("$.shortUrl").value("/api/short-urls/abc1234"))
                .andExpect(jsonPath("$.longUrl").value("https://example.com/landing"));
    }

    @Test
    void createRejectsNonHttpUrl() throws Exception {
        UrlCodecService urlCodecService = Mockito.mock(UrlCodecService.class);
        MockMvc mockMvc = createMockMvc(urlCodecService);

        mockMvc.perform(post("/api/short-urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "longUrl": "ftp://example.com/file"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details.longUrl").value("longUrl must start with http:// or https://"));

        verify(urlCodecService, never()).createOrGet(anyString());
    }

    @Test
    void createLinkReturnsApiEnvelope() throws Exception {
        UrlCodecService urlCodecService = Mockito.mock(UrlCodecService.class);
        MockMvc mockMvc = createMockMvc(urlCodecService);

        Mockito.when(urlCodecService.createOrGet("https://example.com/landing"))
                .thenReturn(new UrlMapping("https://example.com/landing", "abc1234"));

        mockMvc.perform(post("/api/v1/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateLinkRequest(
                                "https://example.com/landing",
                                "Spring campaign"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.data.slug").value("abc1234"))
                .andExpect(jsonPath("$.data.short_url").value("/r/abc1234"))
                .andExpect(jsonPath("$.data.long_url").value("https://example.com/landing"))
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void createLinkValidatesSchemaFields() throws Exception {
        UrlCodecService urlCodecService = Mockito.mock(UrlCodecService.class);
        MockMvc mockMvc = createMockMvc(urlCodecService);

        mockMvc.perform(post("/api/v1/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "long_url": "ftp://example.com/file",
                                  "title": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details.long_url").exists())
                .andExpect(jsonPath("$.error.details.title").exists());

        verify(urlCodecService, never()).createOrGet(anyString());
    }

    @Test
    void redirectAliasResolvesSlug() throws Exception {
        UrlCodecService urlCodecService = Mockito.mock(UrlCodecService.class);
        MockMvc mockMvc = createMockMvc(urlCodecService);

        Mockito.when(urlCodecService.resolve("abc1234"))
                .thenReturn(new UrlMapping("https://example.com/landing", "abc1234"));

        mockMvc.perform(get("/r/abc1234"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("https://example.com/landing"));
    }
}
