package com.linkflow.api.ws.config;

import com.linkflow.api.ws.service.WsTokenService;
import com.linkflow.api.ws.websocket.RealtimeWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class RealtimeWebSocketConfig implements WebSocketConfigurer {

    private final WsTokenService wsTokenService;

    public RealtimeWebSocketConfig(WsTokenService wsTokenService) {
        this.wsTokenService = wsTokenService;
    }

    /**
     * 注册实时 WebSocket 入口
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new RealtimeWebSocketHandler(wsTokenService), "/ws/realtime")
                .setAllowedOrigins("*");
    }
}
