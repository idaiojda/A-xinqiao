package com.example.xinqiaobackend.config;

import com.example.xinqiaobackend.ws.ChatHandshakeInterceptor;
import com.example.xinqiaobackend.ws.ChatWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new ChatWebSocketHandler(), "/ws/chat")
                .addInterceptors(new ChatHandshakeInterceptor())
                .setAllowedOrigins("*");
    }
}

