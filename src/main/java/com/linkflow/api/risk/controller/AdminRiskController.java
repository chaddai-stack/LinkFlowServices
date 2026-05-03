package com.linkflow.api.risk.controller;

import com.linkflow.api.common.response.ApiResponse;
import com.linkflow.api.risk.dto.BlacklistRequest;
import com.linkflow.api.risk.dto.RiskReviewRequest;
import com.linkflow.api.risk.service.RiskService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/risk")
@SecurityRequirement(name = "bearerAuth")
public class AdminRiskController {

    private final RiskService riskService;

    public AdminRiskController(RiskService riskService) {
        this.riskService = riskService;
    }

    /**
     * 审核风险告警
     */
    @PostMapping("/alerts/{alert_id}/review")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reviewAlert(
            @PathVariable("alert_id") UUID alertId,
            @Valid @RequestBody RiskReviewRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                riskService.reviewAlert(alertId, request.action(), request.comment())
        ));
    }

    /**
     * 加入域名黑名单
     */
    @PostMapping("/blacklist/domain")
    public ResponseEntity<ApiResponse<Map<String, Object>>> blacklistDomain(
            @Valid @RequestBody BlacklistRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(riskService.addDomainBlacklist(request)));
    }

    /**
     * 加入 URL 黑名单
     */
    @PostMapping("/blacklist/url")
    public ResponseEntity<ApiResponse<Map<String, Object>>> blacklistUrl(
            @Valid @RequestBody BlacklistRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(riskService.addUrlBlacklist(request)));
    }
}
