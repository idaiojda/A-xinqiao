package com.example.xinqiaobackend.ws;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class RedisChatBroker implements MessageListener {
    private final StringRedisTemplate template;
    private final ConcurrentHashMap<String, CopyOnWriteArraySet<WebSocketSession>> sessionsByGroup = new ConcurrentHashMap<>();

    public RedisChatBroker(StringRedisTemplate template, RedisMessageListenerContainer container) {
        this.template = template;
        if (container != null) {
            container.addMessageListener(this, new PatternTopic("chat:group:*"));
        }
    }

    public void register(String group, WebSocketSession session) {
        sessionsByGroup.computeIfAbsent(group, k -> new CopyOnWriteArraySet<>()).add(session);
    }

    public void unregister(String group, WebSocketSession session) {
        Set<WebSocketSession> set = sessionsByGroup.get(group);
        if (set != null) {
            set.remove(session);
            if (set.isEmpty()) sessionsByGroup.remove(group);
        }
    }

    public void publish(String group, String payload) {
        template.convertAndSend("chat:group:" + group, payload);
        String key = "chat:history:" + group;
        template.opsForList().leftPush(key, payload);
        template.opsForList().trim(key, 0, 99);
    }

    public java.util.List<String> history(String group, int limit) {
        String key = "chat:history:" + group;
        Long size = template.opsForList().size(key);
        int end = Math.max(0, Math.min(limit - 1, size == null ? 0 : size.intValue() - 1));
        return template.opsForList().range(key, 0, end);
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        String prefix = "chat:group:";
        String group = channel.startsWith(prefix) ? channel.substring(prefix.length()) : "default";
        Set<WebSocketSession> set = sessionsByGroup.getOrDefault(group, new CopyOnWriteArraySet<>());
        for (WebSocketSession s : set) {
            if (s.isOpen()) {
                try { s.sendMessage(new TextMessage(payload)); } catch (IOException ignored) {}
            }
        }
    }
}

