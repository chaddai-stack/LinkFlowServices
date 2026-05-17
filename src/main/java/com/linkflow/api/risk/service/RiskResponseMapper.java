package com.linkflow.api.risk.service;

import java.util.Arrays;
import java.util.List;

public final class RiskResponseMapper {

    private RiskResponseMapper() {
    }

    public static String joinCodes(List<String> values) {
        return String.join(",", values);
    }

    public static List<String> splitCodes(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }
}
