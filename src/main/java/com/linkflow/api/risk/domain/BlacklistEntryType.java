package com.linkflow.api.risk.domain;

import java.util.Locale;

public enum BlacklistEntryType {
    DOMAIN,
    URL;

    public String wireValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
