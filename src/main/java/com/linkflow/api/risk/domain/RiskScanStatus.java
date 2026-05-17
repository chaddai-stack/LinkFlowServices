package com.linkflow.api.risk.domain;

import java.util.Locale;

public enum RiskScanStatus {
    QUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED;

    public String wireValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
