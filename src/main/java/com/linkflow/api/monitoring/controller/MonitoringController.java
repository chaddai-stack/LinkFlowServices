package com.linkflow.api.monitoring.controller;

import com.linkflow.api.common.response.ApiResponse;
import com.linkflow.api.monitoring.service.MonitoringService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
@RequestMapping("/api/v1/monitoring")
@SecurityRequirement(name = "bearerAuth")
public class MonitoringController {

    private final MonitoringService monitoringService;

    public MonitoringController(MonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    /**
     * 总体健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        return ResponseEntity.ok(ApiResponse.success(monitoringService.health()));
    }

    /**
     * 服务依赖状态汇总
     */
    @GetMapping("/services")
    public ResponseEntity<ApiResponse<Map<String, Object>>> services() {
        return ResponseEntity.ok(ApiResponse.success(monitoringService.services()));
    }

    /**
     * Redis 状态
     */
    @GetMapping("/redis")
    public ResponseEntity<ApiResponse<Map<String, Object>>> redis() {
        return ResponseEntity.ok(ApiResponse.success(monitoringService.redis()));
    }

    /**
     * Kafka 状态
     */
    @GetMapping("/kafka")
    public ResponseEntity<ApiResponse<Map<String, Object>>> kafka() {
        return ResponseEntity.ok(ApiResponse.success(monitoringService.kafka()));
    }

    /**
     * RabbitMQ 状态
     */
    @GetMapping("/rabbitmq")
    public ResponseEntity<ApiResponse<Map<String, Object>>> rabbitmq() {
        return ResponseEntity.ok(ApiResponse.success(monitoringService.rabbitmq()));
    }

    /**
     * Flink 作业状态
     */
    @GetMapping("/flink/jobs")
    public ResponseEntity<ApiResponse<Map<String, Object>>> flinkJobs() {
        return ResponseEntity.ok(ApiResponse.success(monitoringService.flinkJobs()));
    }

    /**
     * 指标时间序列
     */
    @GetMapping("/metrics/timeseries")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> metricTimeseries(
            @RequestParam @Pattern(regexp = "^[a-zA-Z0-9_.-]{1,80}$") String metric,
            @RequestParam(defaultValue = "1h") @Pattern(regexp = "^(5m|15m|1h|24h)$") String window
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                monitoringService.metricTimeseries(metric, window),
                Map.of("metric", metric, "window", window)
        ));
    }
}
