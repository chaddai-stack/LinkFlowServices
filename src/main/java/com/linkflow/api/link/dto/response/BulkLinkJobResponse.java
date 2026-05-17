package com.linkflow.api.link.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.linkflow.api.link.domain.BulkLinkJob;
import com.linkflow.api.link.domain.BulkLinkItem;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record BulkLinkJobResponse(
        @JsonProperty("job_id")
        UUID jobId,
        String status,
        int total,
        int succeeded,
        int failed,
        List<BulkLinkItemResponse> items,
        @JsonProperty("created_at")
        OffsetDateTime createdAt,
        @JsonProperty("updated_at")
        OffsetDateTime updatedAt
) {
    public static BulkLinkJobResponse from(BulkLinkJob job, List<BulkLinkItem> items, String publicBaseUrl) {
        return new BulkLinkJobResponse(
                job.getId(),
                job.getStatus().wireValue(),
                job.getTotalCount(),
                job.getSucceededCount(),
                job.getFailedCount(),
                items.stream()
                        .map(item -> BulkLinkItemResponse.from(item, publicBaseUrl))
                        .toList(),
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }
}
