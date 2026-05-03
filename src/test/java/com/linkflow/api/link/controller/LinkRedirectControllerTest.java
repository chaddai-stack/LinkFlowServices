package com.linkflow.api.link.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkflow.api.common.exception.ApiExceptionHandler;
import com.linkflow.api.common.web.RequestIdFilter;
import com.linkflow.api.link.domain.UrlMapping;
import com.linkflow.api.link.service.LinkRedirectService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LinkRedirectControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc createMockMvc(LinkRedirectService linkRedirectService) {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        return MockMvcBuilders.standaloneSetup(new LinkRedirectController(linkRedirectService))
                .setControllerAdvice(new ApiExceptionHandler())
                .addFilters(new RequestIdFilter())
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void redirectResolvesBackHalfFromPrefixedRoute() throws Exception {
        LinkRedirectService linkRedirectService = Mockito.mock(LinkRedirectService.class);
        MockMvc mockMvc = createMockMvc(linkRedirectService);

        Mockito.when(linkRedirectService.resolve("abc1234"))
                .thenReturn(new UrlMapping("https://example.com/landing", "abc1234"));

        mockMvc.perform(get("/r/abc1234"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("https://example.com/landing"));
    }

    @Test
    void rootRedirectResolvesBackHalf() throws Exception {
        LinkRedirectService linkRedirectService = Mockito.mock(LinkRedirectService.class);
        MockMvc mockMvc = createMockMvc(linkRedirectService);

        Mockito.when(linkRedirectService.resolve("abc1234"))
                .thenReturn(new UrlMapping("https://example.com/landing", "abc1234"));

        mockMvc.perform(get("/abc1234"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("https://example.com/landing"));
    }
}
