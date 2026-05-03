package com.linkflow.api.risk.controller;

import com.linkflow.api.common.response.ApiResponse;
import com.linkflow.api.risk.dto.RiskAlertResponse;
import com.linkflow.api.risk.dto.RiskScanTaskRequest;
import com.linkflow.api.risk.dto.RiskScanTaskResponse;
import com.linkflow.api.risk.service.RiskService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/risk")
@SecurityRequirement(name = "bearerAuth")
public class RiskController {

    private final RiskService riskService;

    public RiskController(RiskService riskService) {
        this.riskService = riskService;
    }

    /**
     * 查询风险告警列表
     *
     * 当前风险告警存储未落库，Service 返回空列表和分页 meta，保持前端契约稳定。
     */
    @GetMapping("/alerts")
    public ResponseEntity<ApiResponse<List<RiskAlertResponse>>> alerts(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) @Pattern(regexp = "^(unknown|low|medium|high|critical)$") String risk_level,
            @RequestParam(required = false) @Pattern(regexp = "^(pending|approved|blocked|blacklisted)$") String status,
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                riskService.listAlerts(risk_level, status, search),
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

    /**
     * 查询单个风险告警
     */
    @GetMapping("/alerts/{alert_id}")
    public ResponseEntity<ApiResponse<RiskAlertResponse>> alert(@PathVariable("alert_id") UUID alertId) {
        return ResponseEntity.ok(ApiResponse.success(riskService.getAlert(alertId)));
    }

    /**
     * 创建风险扫描任务
     */
    @PostMapping("/scan/tasks")
    public ResponseEntity<ApiResponse<RiskScanTaskResponse>> createScanTask(
            @Valid @RequestBody RiskScanTaskRequest request
    ) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(riskService.createScanTask(request)));
    }

    /**
     * 查询风险扫描任务
     */
    @GetMapping("/scan/tasks/{task_id}")
    public ResponseEntity<ApiResponse<RiskScanTaskResponse>> scanTask(@PathVariable("task_id") UUID taskId) {
        return ResponseEntity.ok(ApiResponse.success(riskService.getScanTask(taskId)));
    }
}
