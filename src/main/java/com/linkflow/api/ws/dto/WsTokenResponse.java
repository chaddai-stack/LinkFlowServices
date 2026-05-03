package com.linkflow.api.ws.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

/**
 * WebSocket 令牌 响应
 */
public record WsTokenResponse(
        String token,
        @JsonProperty("expires_at")
        OffsetDateTime expiresAt
) {
}
