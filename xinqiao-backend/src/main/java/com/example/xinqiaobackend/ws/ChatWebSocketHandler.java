package com.example.xinqiaobackend.ws;

import com.example.xinqiaobackend.entity.ConsultMessage;
import com.example.xinqiaobackend.repository.ConsultMessageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.*;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    private final RedisChatBroker broker;
    private final ConsultMessageRepository consultMessageRepository;
    private final ObjectMapper objectMapper;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ChatWebSocketHandler.class);

    public ChatWebSocketHandler(RedisChatBroker broker, ConsultMessageRepository consultMessageRepository, ObjectMapper objectMapper) {
        this.broker = broker;
        this.consultMessageRepository = consultMessageRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Object g = session.getAttributes().get("group");
        String group = g instanceof String ? (String) g : "default";
        Object u = session.getAttributes().get("user");
        String user = u instanceof String ? (String) u : "anonymous";
        log.info("WebSocket连接建立 - group: {}, user: {}, sessionId: {}", group, user, session.getId());
        broker.register(group, session);
        
        // 从数据库加载历史消息
        List<String> history = loadHistoryFromDatabase(group);
        log.info("加载历史消息 - group: {}, 消息数: {}", group, history.size());
        try {
            for (String msg : history) {
                session.sendMessage(new TextMessage(msg));
            }
        } catch (Exception e) {
            log.error("发送历史消息失败", e);
        }
    }

    private List<String> loadHistoryFromDatabase(String group) {
        List<String> result = new ArrayList<>();
        if (group == null || !group.startsWith("consult_")) {
            return result;
        }
        
        try {
            Long appointmentId = Long.parseLong(group.substring(8));
            List<ConsultMessage> messages = consultMessageRepository.findByAppointmentIdOrderByTsMsDesc(appointmentId, PageRequest.of(0, 50));
            Collections.reverse(messages); // 按时间正序
            
            for (ConsultMessage m : messages) {
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("id", m.getId() == null ? null : m.getId().toString());
                payload.put("group", group);
                payload.put("user", m.getSender());
                payload.put("type", m.getType());
                if (m.getContent() != null) payload.put("text", m.getContent());
                if (m.getUrl() != null) payload.put("url", m.getUrl());
                payload.put("ts", m.getTsMs());
                
                try {
                    result.add(objectMapper.writeValueAsString(payload));
                } catch (Exception e) {
                    log.error("序列化消息失败", e);
                }
            }
        } catch (Exception e) {
            log.error("从数据库加载历史消息失败: {}", e.getMessage());
        }
        
        return result;
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
