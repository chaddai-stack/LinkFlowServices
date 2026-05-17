package com.linkflow.api.link.service;

import java.util.List;

public record ClassificationResult(
        String category,
        double confidence,
        List<String> labels,
        String source
) {
}
