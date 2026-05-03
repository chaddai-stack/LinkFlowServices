package com.linkflow.api.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.linkflow.api.auth.domain.User;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

/**
 * 对外用户信息
 *
 * 不包含 passwordHash，只暴露前端展示和权限判断需要的字段。
 */
public record AuthUserResponse(
        UUID id,
        String email,
        String username,
        String role,
        String status,
        @JsonProperty("created_at")
        OffsetDateTime createdAt
) {
    /**
     * 将用户实体转换为公开 DTO。
     */
    public static AuthUserResponse from(User user) {
        return new AuthUserResponse(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getRole().name().toLowerCase(Locale.ROOT),
                user.getStatus().name().toLowerCase(Locale.ROOT),
                user.getCreatedAt()
        );
    }
}
