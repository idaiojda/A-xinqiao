package com.example.xinqiaobackend.modules.application.controller;

import com.example.xinqiaobackend.api.ApiResponse;
import com.example.xinqiaobackend.api.ErrorCode;
import com.example.xinqiaobackend.modules.application.entity.CounselorApplication;
import com.example.xinqiaobackend.modules.application.service.ApplicationService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {
    
    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    /**
     * 提交咨询师认证申请
     * 使用 Service 层处理业务逻辑，包含悲观锁防止并发重复提交
     */
    @PostMapping
    public ApiResponse<Object> submit(@RequestBody CounselorApplication payload) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return ApiResponse.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        String username = auth.getName();
        return applicationService.submitApplication(username, payload);
    }

    /**
     * 查询当前用户的所有申请记录
     */
    @GetMapping("/me")
    public ApiResponse<List<CounselorApplication>> myApplications() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return ApiResponse.error(ErrorCode.UNAUTHORIZED, "未登录");
        }
        String username = auth.getName();
        return applicationService.getMyApplications(username);
    }

    /**
     * 获取自动审批规则说明
     */
    @GetMapping("/auto-approval-rules")
    public ApiResponse<String> getAutoApprovalRules() {
        return applicationService.getAutoApprovalRules();
    }
}
