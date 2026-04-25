package com.linkflow.api.link.domain;

import com.linkflow.api.common.error.ApiException;
import org.springframework.http.HttpStatus;

import java.util.Locale;
import java.util.Map;

public enum LinkStatus {
    ACTIVE,
    PAUSED,
    EXPIRED,
    BLOCKED;

    public String wireValue() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static LinkStatus fromWireValue(String value) {
        if (value == null || value.isBlank()) {
            throw invalidStatus(value);
        }

        try {
            return LinkStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw invalidStatus(value);
        }
    }

    private static ApiException invalidStatus(String value) {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "Request validation failed.",
                Map.of("status", "must be one of active, paused, expired, blocked", "value", value == null ? "" : value)
        );
    }
}
