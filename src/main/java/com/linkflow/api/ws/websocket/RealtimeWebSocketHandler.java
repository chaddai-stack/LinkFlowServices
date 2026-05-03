package com.linkflow.api.ws.websocket;

import com.linkflow.api.ws.service.WsTokenService;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

public class RealtimeWebSocketHandler extends TextWebSocketHandler {

    private final WsTokenService wsTokenService;

    public RealtimeWebSocketHandler(WsTokenService wsTokenService) {
        this.wsTokenService = wsTokenService;
    }

    /**
     * 建立 WebSocket 连接
     *
     * 握手 URL 中必须带 令牌；校验通过后发送 connection.ready，供前端确认实时通道可用。
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        String token = UriComponentsBuilder.fromUri(session.getUri())
                .build()
                .getQueryParams()
                .getFirst("token");

        if (!wsTokenService.isValid(token)) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("invalid or expired token"));
            return;
        }

        session.sendMessage(new TextMessage("""
                {"event":"connection.ready","data":{"status":"connected"}}
                """.strip()));
    }
}
