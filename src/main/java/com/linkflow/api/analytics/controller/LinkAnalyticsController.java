package com.linkflow.api.analytics.controller;

import com.linkflow.api.analytics.dto.AccessLogResponse;
import com.linkflow.api.analytics.dto.AnalyticsBreakdownItemResponse;
import com.linkflow.api.analytics.dto.AnalyticsSummaryResponse;
import com.linkflow.api.analytics.dto.AnalyticsTimeseriesPointResponse;
import com.linkflow.api.analytics.service.LinkAnalyticsService;
import com.linkflow.api.common.error.ApiException;
import com.linkflow.api.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/links/{link_id}")
@SecurityRequirement(name = "bearerAuth")
public class LinkAnalyticsController {

    private final LinkAnalyticsService linkAnalyticsService;

    public LinkAnalyticsController(LinkAnalyticsService linkAnalyticsService) {
        this.linkAnalyticsService = linkAnalyticsService;
    }

    /**
     * 链接分析汇总
     */
    @GetMapping("/analytics/summary")
    public ResponseEntity<ApiResponse<AnalyticsSummaryResponse>> summary(
            @PathVariable("link_id") UUID linkId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to
    ) {
        validateTimeRange(from, to);
        return ResponseEntity.ok(ApiResponse.success(linkAnalyticsService.getSummary(linkId, from, to)));
    }

    /**
     * 链接点击趋势
     */
    @GetMapping("/analytics/timeseries")
    public ResponseEntity<ApiResponse<List<AnalyticsTimeseriesPointResponse>>> timeseries(
            @PathVariable("link_id") UUID linkId,
            @RequestParam(defaultValue = "1h") @Pattern(regexp = "^(1m|5m|15m|1h|1d)$") String granularity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to
    ) {
        validateTimeRange(from, to);
        return ResponseEntity.ok(ApiResponse.success(
                linkAnalyticsService.getTimeseries(linkId, granularity, from, to),
                Map.of("granularity", granularity)
        ));
    }

    /**
     * 设备维度分析
     */
    @GetMapping("/analytics/devices")
    public ResponseEntity<ApiResponse<List<AnalyticsBreakdownItemResponse>>> devices(
            @PathVariable("link_id") UUID linkId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to
    ) {
        validateTimeRange(from, to);
        return ResponseEntity.ok(ApiResponse.success(linkAnalyticsService.getDevices(linkId, from, to)));
    }

    /**
     * 浏览器维度分析
     */
    @GetMapping("/analytics/browsers")
    public ResponseEntity<ApiResponse<List<AnalyticsBreakdownItemResponse>>> browsers(
            @PathVariable("link_id") UUID linkId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to
    ) {
        validateTimeRange(from, to);
        return ResponseEntity.ok(ApiResponse.success(linkAnalyticsService.getBrowsers(linkId, from, to)));
    }

    /**
     * 地理位置维度分析
     */
    @GetMapping("/analytics/locations")
    public ResponseEntity<ApiResponse<List<AnalyticsBreakdownItemResponse>>> locations(
            @PathVariable("link_id") UUID linkId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to
    ) {
        validateTimeRange(from, to);
        return ResponseEntity.ok(ApiResponse.success(linkAnalyticsService.getLocations(linkId, from, to)));
    }

    /**
     * 访问日志
     */
    @GetMapping("/access-logs")
    public ResponseEntity<ApiResponse<List<AccessLogResponse>>> accessLogs(
            @PathVariable("link_id") UUID linkId,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                linkAnalyticsService.getAccessLogs(linkId, page, size),
                Map.of(
                        "page", Map.of(
                                "page", page,
                                "size", size,
                                "total_elements", 0,
                                "total_pages", 0,
                                "has_next", false
                        )
                )
        ));
    }

    private void validateTimeRange(OffsetDateTime from, OffsetDateTime to) {
        // 与 Dashboard 保持一致：时间范围必须完整，避免半开区间造成前端和后端理解不一致。
        if ((from == null) != (to == null)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "Request validation failed.",
                    Map.of(
                            "from", "from and to must be provided together.",
                            "to", "from and to must be provided together."
                    )
            );
        }
        if (from != null && !from.isBefore(to)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "Request validation failed.",
                    Map.of("from", "from must be before to")
            );
        }
    }
}
