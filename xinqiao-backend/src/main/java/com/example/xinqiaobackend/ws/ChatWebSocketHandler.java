package com.example.xinqiaobackend.ws;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    private final RedisChatBroker broker;

    public ChatWebSocketHandler(RedisChatBroker broker) { this.broker = broker; }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Object g = session.getAttributes().get("group");
        String group = g instanceof String ? (String) g : "default";
        broker.register(group, session);
        List<String> history = broker.history(group, 20);
        try {
            for (String msg : history) { session.sendMessage(new TextMessage(msg)); }
        } catch (Exception ignored) {}
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String text = message.getPayload();
        Object g = session.getAttributes().get("group");
        String group = g instanceof String ? (String) g : "default";
        Object u = session.getAttributes().get("user");
        String user = u instanceof String ? (String) u : "anonymous";
        long ts = System.currentTimeMillis();
        String payload = "{\"group\":\"" + group + "\",\"user\":\"" + user + "\",\"text\":\"" + text.replace("\"", "\\\"") + "\",\"ts\":" + ts + "}";
        broker.publish(group, payload);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Object g = session.getAttributes().get("group");
        String group = g instanceof String ? (String) g : "default";
        broker.unregister(group, session);
    }
}
