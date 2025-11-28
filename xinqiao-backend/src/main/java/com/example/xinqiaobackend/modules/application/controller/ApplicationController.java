package com.example.xinqiaobackend.modules.application.controller;

import com.example.xinqiaobackend.api.ApiResponse;
import com.example.xinqiaobackend.api.ErrorCode;
import com.example.xinqiaobackend.modules.application.entity.CounselorApplication;
import com.example.xinqiaobackend.entity.User;
import com.example.xinqiaobackend.modules.application.repository.CounselorApplicationRepository;
import com.example.xinqiaobackend.repository.UserRepository;
import com.example.xinqiaobackend.repository.NotificationRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {
    private final CounselorApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    public ApplicationController(CounselorApplicationRepository applicationRepository, UserRepository userRepository, NotificationRepository notificationRepository) {
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
    }

    @PostMapping
    @Transactional
    public ApiResponse<Object> submit(@RequestBody CounselorApplication payload) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return ApiResponse.error(ErrorCode.UNAUTHORIZED, "未登录");
        String username = auth.getName();
        Optional<User> opt = userRepository.findByUsername(username);
        if (!opt.isPresent()) return ApiResponse.error(ErrorCode.NOT_FOUND, "用户不存在");
        User u = opt.get();
        Optional<CounselorApplication> pending = applicationRepository.findPendingByUser(u.getId());
        if (pending.isPresent()) return ApiResponse.error(ErrorCode.CONFLICT, "已有待审核申请");

        if (payload.getRealName() == null || payload.getRealName().trim().isEmpty()) {
            return ApiResponse.error(ErrorCode.BAD_REQUEST, "真实姓名必填");
        }
        if (payload.getQualificationType() == null || payload.getQualificationType().trim().isEmpty()) {
            return ApiResponse.error(ErrorCode.BAD_REQUEST, "资质类型必填");
        }

        CounselorApplication a = new CounselorApplication();
        a.setUserId(u.getId());
        a.setRealName(payload.getRealName());
        a.setIdCard(payload.getIdCard());
        a.setPhone(payload.getPhone());
        a.setQualificationType(payload.getQualificationType());
        a.setCertificateNo(payload.getCertificateNo());
        a.setYears(payload.getYears());
        a.setExpertise(payload.getExpertise());
        a.setMaterials(payload.getMaterials());
        a.setIntro(payload.getIntro());
        a.setStatus("pending");
        a.setRejectedReason(null);
        a.setCreatedAt(LocalDateTime.now());
        a.setUpdatedAt(LocalDateTime.now());
        applicationRepository.save(a);

        com.example.xinqiaobackend.entity.Notification n = new com.example.xinqiaobackend.entity.Notification();
        n.setUsername(u.getUsername());
        n.setType("application");
        n.setTitle("申请提交成功");
        n.setContent("已收到您的咨询师申请，正在审核。");
        notificationRepository.save(n);

        u.setReviewStatus("PENDING");
        userRepository.save(u);
        return ApiResponse.successMessage("已提交，等待审核");
    }

    @GetMapping("/me")
    public ApiResponse<List<CounselorApplication>> myApplications() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) return ApiResponse.error(ErrorCode.UNAUTHORIZED, "未登录");
        String username = auth.getName();
        Optional<User> opt = userRepository.findByUsername(username);
        if (!opt.isPresent()) return ApiResponse.error(ErrorCode.NOT_FOUND, "用户不存在");
        User u = opt.get();
        List<CounselorApplication> list = applicationRepository.findByUserId(u.getId());
        return ApiResponse.success(list);
    }
}
