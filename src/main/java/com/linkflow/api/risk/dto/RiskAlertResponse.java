package com.linkflow.api.risk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.linkflow.api.risk.domain.RiskAlert;
import com.linkflow.api.risk.service.RiskResponseMapper;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 风险告警响应。
 */
public record RiskAlertResponse(
        UUID id,
        @JsonProperty("link_id")
        UUID linkId,
        @JsonProperty("short_url")
        String shortUrl,
        String title,
        @JsonProperty("risk_level")
        String riskLevel,
        @JsonProperty("risk_score")
        int riskScore,
        List<String> reasons,
        String status,
        @JsonProperty("detected_at")
        OffsetDateTime detectedAt,
        String reporter
) {
    public static RiskAlertResponse from(RiskAlert alert, String publicBaseUrl) {
        var link = alert.getLink();
        String baseUrl = publicBaseUrl == null ? "" : publicBaseUrl.replaceAll("/+$", "");
        String shortUrl = link == null ? null : baseUrl + "/" + link.getBackHalf();
        return new RiskAlertResponse(
                alert.getId(),
                link == null ? null : link.getPublicId(),
                shortUrl,
                alert.getTitle(),
                alert.getRiskLevel().wireValue(),
                alert.getRiskScore(),
                RiskResponseMapper.splitCodes(alert.getReasonCodes()),
                alert.getStatus().wireValue(),
                alert.getCreatedAt(),
                alert.getReporter()
        );
    }
}
