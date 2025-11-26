package com.example.xinqiaobackend.controller;

import com.example.xinqiaobackend.repository.AppointmentRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportController {
    private final AppointmentRepository appointmentRepository;

    public AdminReportController(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    @GetMapping("/overview")
    public Map<String, Object> overview() {
        Map<String, Object> m = new HashMap<>();
        m.put("appointments", appointmentRepository.count());
        m.put("aiUsage", 0);
        m.put("activeUsers", 0);
        m.put("assessmentUsage", 0);
        return m;
    }
}