package com.example.xinqiaobackend.modules.application.controller;

import com.example.xinqiaobackend.api.ApiResponse;
import com.example.xinqiaobackend.api.ErrorCode;
import com.example.xinqiaobackend.modules.application.entity.CounselorApplication;
import com.example.xinqiaobackend.entity.User;
import com.example.xinqiaobackend.modules.application.repository.CounselorApplicationRepository;
import com.example.xinqiaobackend.repository.UserRepository;
import com.example.xinqiaobackend.repository.NotificationRepository;
import com.example.xinqiaobackend.repository.CounselorProfileRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/applications")
@PreAuthorize("hasRole('ADMIN')")
public class AdminApplicationsController {
    private final CounselorApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final CounselorProfileRepository counselorProfileRepository;

    public AdminApplicationsController(CounselorApplicationRepository applicationRepository, UserRepository userRepository, NotificationRepository notificationRepository, CounselorProfileRepository counselorProfileRepository) {
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.counselorProfileRepository = counselorProfileRepository;
    }

    @GetMapping
    public ApiResponse<List<CounselorApplication>> list(@RequestParam(required = false) String status,
                                                        @RequestParam(required = false) String query) {
        List<CounselorApplication> list = applicationRepository.adminList(status, query);
        return ApiResponse.success(list);
    }

    @GetMapping("/{id}")
    public ApiResponse<CounselorApplication> detail(@PathVariable Long id) {
        Optional<CounselorApplication> opt = applicationRepository.findById(id);
        if (!opt.isPresent()) return ApiResponse.error(ErrorCode.NOT_FOUND, "未找到");
        return ApiResponse.success(opt.get());
    }

    @PostMapping("/{id}/approve")
    @Transactional
    public ApiResponse<Object> approve(@PathVariable Long id) {
        Optional<CounselorApplication> opt = applicationRepository.findById(id);
        if (!opt.isPresent()) return ApiResponse.error(ErrorCode.NOT_FOUND, "未找到");
        CounselorApplication a = opt.get();
        if (!"pending".equalsIgnoreCase(a.getStatus())) return ApiResponse.error(ErrorCode.CONFLICT, "当前状态不可审批");
        a.setStatus("approved");
        a.setRejectedReason(null);
        a.setUpdatedAt(LocalDateTime.now());
        applicationRepository.save(a);

        Optional<User> uopt = userRepository.findById(a.getUserId());
        if (!uopt.isPresent()) return ApiResponse.error(ErrorCode.NOT_FOUND, "用户不存在");
        User u = uopt.get();
        if (!u.getRoles().contains("COUNSELOR")) u.getRoles().add("COUNSELOR");
        u.setReviewStatus("APPROVED");
        userRepository.save(u);
        com.example.xinqiaobackend.entity.CounselorProfile p = new com.example.xinqiaobackend.entity.CounselorProfile();
        p.setUserId(u.getId());
        p.setUsername(u.getUsername());
        p.setTitle(a.getRealName());
        p.setDefaultMode("text");
        counselorProfileRepository.save(p);
        com.example.xinqiaobackend.entity.Notification n = new com.example.xinqiaobackend.entity.Notification();
        n.setUsername(u.getUsername());
        n.setType("application");
        n.setTitle("审核通过");
        n.setContent("恭喜，您的咨询师申请已通过。");
        notificationRepository.save(n);
        return ApiResponse.successMessage("已通过");
    }

    public static class RejectBody { public String reason; }

    @PostMapping("/{id}/reject")
    @Transactional
    public ApiResponse<Object> reject(@PathVariable Long id, @RequestBody RejectBody body) {
        Optional<CounselorApplication> opt = applicationRepository.findById(id);
        if (!opt.isPresent()) return ApiResponse.error(ErrorCode.NOT_FOUND, "未找到");
        CounselorApplication a = opt.get();
        if (!"pending".equalsIgnoreCase(a.getStatus())) return ApiResponse.error(ErrorCode.CONFLICT, "当前状态不可驳回");
        a.setStatus("rejected");
        a.setRejectedReason(body == null ? null : body.reason);
        a.setUpdatedAt(LocalDateTime.now());
        applicationRepository.save(a);

        Optional<User> uopt = userRepository.findById(a.getUserId());
        if (!uopt.isPresent()) return ApiResponse.error(ErrorCode.NOT_FOUND, "用户不存在");
        User u = uopt.get();
        u.setReviewStatus("REJECTED");
        userRepository.save(u);
        com.example.xinqiaobackend.entity.Notification n = new com.example.xinqiaobackend.entity.Notification();
        n.setUsername(u.getUsername());
        n.setType("application");
        n.setTitle("审核未通过");
        n.setContent(a.getRejectedReason() == null ? "请完善资料后重新申请" : a.getRejectedReason());
        notificationRepository.save(n);
        return ApiResponse.successMessage("已驳回");
    }

    public static class BatchIds { public java.util.List<Long> ids; public String reason; }

    @PostMapping("/batch/approve")
    public ApiResponse<Object> batchApprove(@RequestBody BatchIds req) {
        if (req == null || req.ids == null || req.ids.isEmpty()) return ApiResponse.error(ErrorCode.BAD_REQUEST, "参数错误");
        for (Long id : req.ids) {
            Optional<CounselorApplication> opt = applicationRepository.findById(id);
            if (!opt.isPresent()) continue;
            CounselorApplication a = opt.get();
            if (!"pending".equalsIgnoreCase(a.getStatus())) continue;
            a.setStatus("approved");
            a.setRejectedReason(null);
            a.setUpdatedAt(LocalDateTime.now());
            applicationRepository.save(a);
            Optional<User> uopt = userRepository.findById(a.getUserId());
            if (!uopt.isPresent()) continue;
            User u = uopt.get();
            if (!u.getRoles().contains("COUNSELOR")) u.getRoles().add("COUNSELOR");
            u.setReviewStatus("APPROVED");
            userRepository.save(u);
            com.example.xinqiaobackend.entity.Notification n = new com.example.xinqiaobackend.entity.Notification();
            n.setUsername(u.getUsername());
            n.setType("application");
            n.setTitle("审核通过");
            n.setContent("恭喜，您的咨询师申请已通过。");
            notificationRepository.save(n);
        }
        return ApiResponse.successMessage("批量通过完成");
    }

    @PostMapping("/batch/reject")
    public ApiResponse<Object> batchReject(@RequestBody BatchIds req) {
        if (req == null || req.ids == null || req.ids.isEmpty()) return ApiResponse.error(ErrorCode.BAD_REQUEST, "参数错误");
        for (Long id : req.ids) {
            Optional<CounselorApplication> opt = applicationRepository.findById(id);
            if (!opt.isPresent()) continue;
            CounselorApplication a = opt.get();
            if (!"pending".equalsIgnoreCase(a.getStatus())) continue;
            a.setStatus("rejected");
            a.setRejectedReason(req.reason);
            a.setUpdatedAt(LocalDateTime.now());
            applicationRepository.save(a);
            Optional<User> uopt = userRepository.findById(a.getUserId());
            if (!uopt.isPresent()) continue;
            User u = uopt.get();
            u.setReviewStatus("REJECTED");
            userRepository.save(u);
            com.example.xinqiaobackend.entity.Notification n = new com.example.xinqiaobackend.entity.Notification();
            n.setUsername(u.getUsername());
            n.setType("application");
            n.setTitle("审核未通过");
            n.setContent(req.reason == null ? "请完善资料后重新申请" : req.reason);
            notificationRepository.save(n);
        }
        return ApiResponse.successMessage("批量驳回完成");
    }

    @GetMapping("/rejectReasons")
    public ApiResponse<java.util.List<String>> rejectReasons() {
        java.util.List<String> list = java.util.Arrays.asList(
                "资料不完整",
                "证书编号有误",
                "资质类型不符合要求",
                "从业年限不足",
                "证明材料不清晰"
        );
        return ApiResponse.success(list);
    }
}
