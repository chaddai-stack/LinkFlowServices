package com.linkflow.api.link.domain;

import java.util.Locale;

public enum BulkLinkJobStatus {
    RUNNING,
    SUCCEEDED,
    PARTIAL_FAILED,
    FAILED;

    public String wireValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
