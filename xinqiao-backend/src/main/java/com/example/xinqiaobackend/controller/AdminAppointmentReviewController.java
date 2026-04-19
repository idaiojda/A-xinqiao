package com.example.xinqiaobackend.controller;

import com.example.xinqiaobackend.entity.Appointment;
import com.example.xinqiaobackend.entity.AppointmentStatus;
import com.example.xinqiaobackend.repository.AppointmentRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/appointments")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAppointmentReviewController {
    private final AppointmentRepository repo;
    private final com.example.xinqiaobackend.repository.ScheduleSlotRepository slotRepo;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    public AdminAppointmentReviewController(AppointmentRepository repo, com.example.xinqiaobackend.repository.ScheduleSlotRepository slotRepo, org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate) {
        this.repo = repo;
        this.slotRepo = slotRepo;
        this.messagingTemplate = messagingTemplate;
    }

    @GetMapping
    public List<Appointment> list(@RequestParam(required = false) String status) {
        if (status == null || status.isEmpty()) {
            return repo.findAll();
        }
        AppointmentStatus st;
        try {
            st = AppointmentStatus.valueOf(status);
        } catch (Exception e) {
            return repo.findAll();
        }
        return repo.findAll().stream().filter(a -> a.getStatus() == st).collect(Collectors.toList());
    }

    @GetMapping("/review")
    public List<Appointment> listReview(@RequestParam(required = false) String status) {
        return list(status);
    }

    @PostMapping("/{id}/approve")
    public com.example.xinqiaobackend.api.ApiResponse<Object> approve(@PathVariable Long id) {
        Appointment a = repo.findById(id).orElse(null);
        if (a == null) return com.example.xinqiaobackend.api.ApiResponse.error(com.example.xinqiaobackend.api.ErrorCode.NOT_FOUND, "未找到");
        java.util.List<com.example.xinqiaobackend.entity.ScheduleSlot> slots = slotRepo
                .findByCounselorUsernameAndStartTimeLessThanEqualAndEndTimeGreaterThanEqualAndAvailableTrue(a.getCounselorUsername(), a.getStartTime(), a.getEndTime());
        if (slots.isEmpty()) {
            return com.example.xinqiaobackend.api.ApiResponse.error(com.example.xinqiaobackend.api.ErrorCode.CONFLICT, "时段不可用或已占用");
        }
        a.setStatus(AppointmentStatus.APPROVED);
        repo.save(a);
        for (com.example.xinqiaobackend.entity.ScheduleSlot s : slots) { s.setAvailable(false); slotRepo.save(s); }
        messagingTemplate.convertAndSend("/topic/appointments/" + a.getUserUsername(), java.util.Collections.singletonMap("status", a.getStatus().name()));
        return com.example.xinqiaobackend.api.ApiResponse.successMessage("已通过");
    }

    @PostMapping("/{id}/reject")
    public com.example.xinqiaobackend.api.ApiResponse<Object> reject(@PathVariable Long id) {
        Appointment a = repo.findById(id).orElse(null);
        if (a == null) return com.example.xinqiaobackend.api.ApiResponse.error(com.example.xinqiaobackend.api.ErrorCode.NOT_FOUND, "未找到");
        a.setStatus(AppointmentStatus.REJECTED);
        repo.save(a);
        messagingTemplate.convertAndSend("/topic/appointments/" + a.getUserUsername(), java.util.Collections.singletonMap("status", a.getStatus().name()));
        return com.example.xinqiaobackend.api.ApiResponse.successMessage("已驳回");
    }

    @PostMapping("/{id}/complete")
    public com.example.xinqiaobackend.api.ApiResponse<Object> complete(@PathVariable Long id) {
        Appointment a = repo.findById(id).orElse(null);
        if (a == null) return com.example.xinqiaobackend.api.ApiResponse.error(com.example.xinqiaobackend.api.ErrorCode.NOT_FOUND, "未找到");
        a.setStatus(AppointmentStatus.COMPLETED);
        repo.save(a);
        messagingTemplate.convertAndSend("/topic/appointments/" + a.getUserUsername(), java.util.Collections.singletonMap("status", a.getStatus().name()));
        return com.example.xinqiaobackend.api.ApiResponse.successMessage("已完成");
    }
}