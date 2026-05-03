package com.linkflow.api.dashboard.controller;

import com.linkflow.api.common.error.ApiException;
import com.linkflow.api.common.response.ApiResponse;
import com.linkflow.api.dashboard.dto.DashboardBreakdownItemResponse;
import com.linkflow.api.dashboard.dto.DashboardSummaryResponse;
import com.linkflow.api.dashboard.dto.DashboardTrendPointResponse;
import com.linkflow.api.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/v1/dashboard")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * 仪表盘总览
     *
     * 当前统计 link 维度的真实数据，点击类指标在事件表接入前保持 0。
     */
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to
    ) {
        validateTimeRange(from, to);
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getSummary(from, to)));
    }

    /**
     * 趋势数据
     */
    @GetMapping("/trends")
    public ResponseEntity<ApiResponse<List<DashboardTrendPointResponse>>> trends(
            @RequestParam(defaultValue = "1h") @Pattern(regexp = "^(1m|5m|15m|1h|1d)$") String granularity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to
    ) {
        validateTimeRange(from, to);
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.getTrends(granularity, from, to),
                Map.of("granularity", granularity)
        ));
    }

    /**
     * 渠道分布
     */
    @GetMapping("/channels")
    public ResponseEntity<ApiResponse<List<DashboardBreakdownItemResponse>>> channels(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to
    ) {
        validateTimeRange(from, to);
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getChannels(from, to)));
    }

    /**
     * 地理分布
     */
    @GetMapping("/locations")
    public ResponseEntity<ApiResponse<List<DashboardBreakdownItemResponse>>> locations(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to
    ) {
        validateTimeRange(from, to);
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getLocations(from, to)));
    }

    private void validateTimeRange(OffsetDateTime from, OffsetDateTime to) {
        // 时间过滤必须成对出现，否则统计区间语义不完整。
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
