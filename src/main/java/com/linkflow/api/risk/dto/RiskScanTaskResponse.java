package com.linkflow.api.risk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.linkflow.api.risk.domain.RiskScanTask;
import com.linkflow.api.risk.service.RiskResponseMapper;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 风险检测任务响应。
 */
public record RiskScanTaskResponse(
        @JsonProperty("task_id")
        UUID taskId,
        @JsonProperty("link_id")
        UUID linkId,
        @JsonProperty("long_url")
        String longUrl,
        String status,
        @JsonProperty("risk_level")
        String riskLevel,
        @JsonProperty("risk_score")
        int riskScore,
        List<String> reasons,
        @JsonProperty("created_at")
        OffsetDateTime createdAt,
        @JsonProperty("finished_at")
        OffsetDateTime finishedAt,
        @JsonProperty("error_message")
        String errorMessage
) {
    public static RiskScanTaskResponse from(RiskScanTask task) {
        UUID linkId = task.getLink() == null ? null : task.getLink().getPublicId();
        return new RiskScanTaskResponse(
                task.getId(),
                linkId,
                task.getLongUrl(),
                task.getStatus().wireValue(),
                task.getRiskLevel().wireValue(),
                task.getRiskScore(),
                RiskResponseMapper.splitCodes(task.getReasonCodes()),
                task.getCreatedAt(),
                task.getFinishedAt(),
                task.getErrorMessage()
        );
    }
}
