package com.example.xinqiaobackend.ws;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class ChatWebSocketHandler extends TextWebSocketHandler {
    private final ConcurrentHashMap<String, CopyOnWriteArraySet<WebSocketSession>> groups = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Object g = session.getAttributes().get("group");
        String group = g instanceof String ? (String) g : "default";
        groups.computeIfAbsent(group, k -> new CopyOnWriteArraySet<>()).add(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Object g = session.getAttributes().get("group");
        String group = g instanceof String ? (String) g : "default";
        Set<WebSocketSession> set = groups.getOrDefault(group, new CopyOnWriteArraySet<>());
        for (WebSocketSession s : set) {
            if (s.isOpen()) {
                try { s.sendMessage(message); } catch (IOException ignored) {}
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Object g = session.getAttributes().get("group");
        String group = g instanceof String ? (String) g : "default";
        Set<WebSocketSession> set = groups.get(group);
        if (set != null) {
            set.remove(session);
            if (set.isEmpty()) groups.remove(group);
        }
    }
}

