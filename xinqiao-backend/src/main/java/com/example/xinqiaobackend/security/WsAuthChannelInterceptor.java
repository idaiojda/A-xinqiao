package com.example.xinqiaobackend.security;

import io.jsonwebtoken.Claims;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class WsAuthChannelInterceptor implements ChannelInterceptor {
    private final JwtUtil jwtUtil;

    public WsAuthChannelInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            List<String> auth = accessor.getNativeHeader("Authorization");
            if (auth != null && !auth.isEmpty()) {
                String h = auth.get(0);
                if (h != null && h.startsWith("Bearer ")) {
                    String token = h.substring(7);
                    try {
                        Claims claims = jwtUtil.parse(token);
                        String username = claims.getSubject();
                        List<String> roles = claims.get("roles", List.class);
                        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                        if (roles != null) {
                            for (String r : roles) authorities.add(new SimpleGrantedAuthority("ROLE_" + r));
                        }
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(username, null, authorities);
                        accessor.setUser(authToken);
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    } catch (Exception ignored) { }
                }
            }
        }
        return message;
    }
}
