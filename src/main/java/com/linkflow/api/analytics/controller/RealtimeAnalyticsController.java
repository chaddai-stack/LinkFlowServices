package com.linkflow.api.analytics.controller;

import com.linkflow.api.analytics.service.RealtimeAnalyticsService;
import com.linkflow.api.common.response.ApiResponse;
import com.linkflow.api.link.dto.LinkSummaryResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/v1/analytics/realtime")
@SecurityRequirement(name = "bearerAuth")
public class RealtimeAnalyticsController {

    private final RealtimeAnalyticsService realtimeAnalyticsService;

    public RealtimeAnalyticsController(RealtimeAnalyticsService realtimeAnalyticsService) {
        this.realtimeAnalyticsService = realtimeAnalyticsService;
    }

    @GetMapping("/hot-links")
    public ResponseEntity<ApiResponse<List<LinkSummaryResponse>>> hotLinks(
            @RequestParam(defaultValue = "15m") @Pattern(regexp = "^(5m|15m|1h|24h)$") String window,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                realtimeAnalyticsService.getHotLinks(window, limit),
                Map.of("window", window, "limit", limit)
        ));
    }
}
