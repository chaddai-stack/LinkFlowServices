package com.linkflow.api.risk.domain;

import java.util.Locale;

public enum RiskLevel {
    UNKNOWN,
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    public String wireValue() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static RiskLevel fromWireValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return RiskLevel.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
