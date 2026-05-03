package com.linkflow.api.ws.controller;

import com.linkflow.api.common.response.ApiResponse;
import com.linkflow.api.ws.dto.WsTokenResponse;
import com.linkflow.api.ws.service.WsTokenService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WsController {

    private final WsTokenService wsTokenService;

    public WsController(WsTokenService wsTokenService) {
        this.wsTokenService = wsTokenService;
    }

    /**
     * 签发 WebSocket 短期 令牌
     *
     * 该 HTTP 接口需要 JWT；真正的 WebSocket 握手通过 query 令牌 完成二次校验。
     */
    @PostMapping("/api/v1/ws/token")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<WsTokenResponse>> issueToken() {
        return ResponseEntity.ok(ApiResponse.success(wsTokenService.issue()));
    }
}
