package com.linkflow.api.risk.domain;

import java.util.Locale;

public enum RiskAlertStatus {
    PENDING,
    APPROVED,
    BLOCKED,
    BLACKLISTED;

    public String wireValue() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static RiskAlertStatus fromWireValue(String value) {
        return RiskAlertStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
