package com.linkflow.api.risk.service;

import com.linkflow.api.risk.domain.RiskLevel;

import java.util.List;

public record RiskInspectionResult(
        RiskLevel riskLevel,
        int riskScore,
        List<String> reasons
) {
}
