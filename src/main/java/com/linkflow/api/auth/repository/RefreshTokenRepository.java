package com.linkflow.api.auth.repository;

import com.linkflow.api.auth.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 刷新令牌仓库。
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * 按原始 令牌 查询
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * 查询用户当前有效 刷新令牌
     */
    List<RefreshToken> findAllByUserIdAndRevokedAtIsNullAndExpiresAtAfter(UUID userId, OffsetDateTime now);
}
