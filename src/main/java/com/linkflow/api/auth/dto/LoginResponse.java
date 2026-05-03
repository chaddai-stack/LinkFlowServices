package com.linkflow.api.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

/**
 * 登录/刷新响应
 *
 * 同时返回短期 访问令牌 和长期 刷新令牌；前端按 expires_at 判断刷新时机。
 */
public record LoginResponse(
        @JsonProperty("access_token")
        String accessToken,
        @JsonProperty("token_type")
        String tokenType,
        @JsonProperty("expires_at")
        OffsetDateTime expiresAt,
        @JsonProperty("refresh_token")
        String refreshToken,
        @JsonProperty("refresh_expires_at")
        OffsetDateTime refreshExpiresAt,
        AuthUserResponse user
) {
    /**
     * 构造 Bearer 令牌 响应
     */
    public static LoginResponse bearer(
            String accessToken,
            OffsetDateTime expiresAt,
            String refreshToken,
            OffsetDateTime refreshExpiresAt,
            AuthUserResponse user
    ) {
        return new LoginResponse(accessToken, "Bearer", expiresAt, refreshToken, refreshExpiresAt, user);
    }
}
