package com.linkflow.api.analytics.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 访问日志响应
 */
public record AccessLogResponse(
        @JsonProperty("event_id")
        UUID eventId,
        @JsonProperty("occurred_at")
        OffsetDateTime occurredAt,
        String ip,
        String country,
        String city,
        @JsonProperty("device_type")
        String deviceType,
        String browser,
        String referrer
) {
}
