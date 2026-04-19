package com.example.xinqiaobackend.controller;

import com.example.xinqiaobackend.api.ApiResponse;
import com.example.xinqiaobackend.service.RevenueShareService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/counselor")
public class CounselorController {
    private final RevenueShareService revenueShareService;

    public CounselorController(RevenueShareService revenueShareService) {
        this.revenueShareService = revenueShareService;
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("counselor-ok");
    }

    @GetMapping("/income/summary")
    @PreAuthorize("hasRole('COUNSELOR')")
    public ApiResponse<Map<String, Object>> getIncomeSummary(Authentication auth) {
        RevenueShareService.CounselorBalanceSummary summary = revenueShareService.getCounselorSummary(auth.getName());
        Map<String, Object> result = new HashMap<>();
        result.put("balance", summary.balance);
        result.put("totalIncome", summary.totalIncome);
        result.put("totalGrossIncome", summary.totalGrossIncome);
        result.put("totalPlatformFee", summary.totalPlatformFee);
        result.put("monthIncome", summary.monthIncome);
        result.put("monthGrossIncome", summary.monthGrossIncome);
        result.put("monthPlatformFee", summary.monthPlatformFee);
        return ApiResponse.success(result);
    }
}