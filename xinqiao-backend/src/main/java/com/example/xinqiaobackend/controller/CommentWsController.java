package com.example.xinqiaobackend.controller;

import com.example.xinqiaobackend.dto.CommentDto;
import com.example.xinqiaobackend.model.CommentMessage;
import com.example.xinqiaobackend.service.JpaCommunityService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

@Controller
public class CommentWsController {
    private final JpaCommunityService jpaService;
    private final SimpMessagingTemplate messagingTemplate;

    public CommentWsController(JpaCommunityService jpaService, SimpMessagingTemplate messagingTemplate) {
        this.jpaService = jpaService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/posts/{postId}/comments")
    public void add(@DestinationVariable("postId") Long postId, CommentMessage message) {
        String username = SecurityContextHolder.getContext().getAuthentication() != null ? SecurityContextHolder.getContext().getAuthentication().getName() : null;
        if (username == null) throw new IllegalArgumentException("unauthorized");
        String text = message != null ? message.getText() : "";
        Long parentId = message != null ? message.getParentId() : null;
        CommentDto dto = jpaService.addPostComment(postId, username, text, parentId);
        messagingTemplate.convertAndSend("/topic/posts/" + postId + "/comments", dto);
    }
}
