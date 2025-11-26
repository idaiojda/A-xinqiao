package com.example.xinqiaobackend.controller;

import com.example.xinqiaobackend.api.ApiResponse;
import com.example.xinqiaobackend.api.ErrorCode;
import com.example.xinqiaobackend.entity.Notification;
import com.example.xinqiaobackend.repository.NotificationRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationRepository repo;

    public NotificationController(NotificationRepository repo) { this.repo = repo; }

    @GetMapping
    public ApiResponse<List<Notification>> list() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return ApiResponse.error(ErrorCode.UNAUTHORIZED, "未登录");
        return ApiResponse.success(repo.findByUsernameOrderByCreatedAtDesc(auth.getName()));
    }

    @PostMapping("/{id}/read")
    public ApiResponse<Object> read(@PathVariable Long id) {
        Notification n = repo.findById(id).orElse(null);
        if (n == null) return ApiResponse.error(ErrorCode.NOT_FOUND, "未找到");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return ApiResponse.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (!auth.getName().equals(n.getUsername())) return ApiResponse.error(ErrorCode.FORBIDDEN, "无权访问");
        n.setRead(true);
        repo.save(n);
        return ApiResponse.successMessage("已读");
    }
}