package com.linkflow.api.link.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.linkflow.api.link.domain.LinkClassification;
import com.linkflow.api.link.service.LinkClassificationService;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record LinkClassificationResponse(
        @JsonProperty("link_id")
        UUID linkId,
        String category,
        double confidence,
        List<String> labels,
        String source,
        @JsonProperty("created_at")
        OffsetDateTime createdAt,
        @JsonProperty("updated_at")
        OffsetDateTime updatedAt
) {
    public static LinkClassificationResponse from(LinkClassification classification) {
        return new LinkClassificationResponse(
                classification.getLink().getPublicId(),
                classification.getCategory(),
                classification.getConfidence(),
                LinkClassificationService.split(classification.getLabels()),
                classification.getSource(),
                classification.getCreatedAt(),
                classification.getUpdatedAt()
        );
    }
}
