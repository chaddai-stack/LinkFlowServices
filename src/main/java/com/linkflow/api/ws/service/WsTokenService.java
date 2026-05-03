package com.linkflow.api.ws.service;

import com.linkflow.api.ws.dto.WsTokenResponse;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WsTokenService {

    private final Map<String, OffsetDateTime> tokens = new ConcurrentHashMap<>();

    /**
     * 签发 10 分钟有效的 WS 令牌
     */
    public WsTokenResponse issue() {
        OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(10);
        String token = UUID.randomUUID().toString();
        tokens.put(token, expiresAt);
        return new WsTokenResponse(token, expiresAt);
    }

    /**
     * 校验 WS 令牌
     *
     * 过期 令牌 会被清理，避免内存 map 长期积累无效数据。
     */
    public boolean isValid(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        OffsetDateTime expiresAt = tokens.get(token);
        if (expiresAt == null) {
            return false;
        }
        if (expiresAt.isBefore(OffsetDateTime.now())) {
            tokens.remove(token);
            return false;
        }
        return true;
    }
}
