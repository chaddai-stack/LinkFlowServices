package com.linkflow.api.link.domain;

import java.util.Locale;

public enum BulkLinkItemStatus {
    PENDING,
    SUCCEEDED,
    FAILED;

    public String wireValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
