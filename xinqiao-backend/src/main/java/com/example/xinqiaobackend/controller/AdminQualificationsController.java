package com.example.xinqiaobackend.controller;

import com.example.xinqiaobackend.entity.User;
import com.example.xinqiaobackend.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/qualifications")
@PreAuthorize("hasRole('ADMIN')")
public class AdminQualificationsController {
    private final UserRepository userRepository;

    public AdminQualificationsController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<User> list(@RequestParam(required = false) String status,
                           @RequestParam(required = false) String query) {
        return userRepository.findByRoleAndStatus("COUNSELOR", status, query);
    }

    @GetMapping("/{id}")
    public com.example.xinqiaobackend.api.ApiResponse<User> detail(@PathVariable Long id) {
        User u = userRepository.findById(id).orElse(null);
        if (u == null) return com.example.xinqiaobackend.api.ApiResponse.error(com.example.xinqiaobackend.api.ErrorCode.NOT_FOUND, "未找到");
        return com.example.xinqiaobackend.api.ApiResponse.success(u);
    }

    @PostMapping("/{id}/approve")
    public com.example.xinqiaobackend.api.ApiResponse<Object> approve(@PathVariable Long id) {
        User u = userRepository.findById(id).orElse(null);
        if (u == null) return com.example.xinqiaobackend.api.ApiResponse.error(com.example.xinqiaobackend.api.ErrorCode.NOT_FOUND, "未找到");
        u.setReviewStatus("APPROVED");
        userRepository.save(u);
        return com.example.xinqiaobackend.api.ApiResponse.successMessage("已通过");
    }

    @PostMapping("/{id}/reject")
    public com.example.xinqiaobackend.api.ApiResponse<Object> reject(@PathVariable Long id) {
        User u = userRepository.findById(id).orElse(null);
        if (u == null) return com.example.xinqiaobackend.api.ApiResponse.error(com.example.xinqiaobackend.api.ErrorCode.NOT_FOUND, "未找到");
        u.setReviewStatus("REJECTED");
        userRepository.save(u);
        return com.example.xinqiaobackend.api.ApiResponse.successMessage("已驳回");
    }
}