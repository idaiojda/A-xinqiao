package com.example.xinqiaobackend.controller;

import com.example.xinqiaobackend.entity.Appointment;
import com.example.xinqiaobackend.entity.AppointmentStatus;
import com.example.xinqiaobackend.entity.ScheduleSlot;
import com.example.xinqiaobackend.repository.AppointmentRepository;
import com.example.xinqiaobackend.repository.ScheduleSlotRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/consult/pro")
public class ConsultProController {
    private final ScheduleSlotRepository slotRepo;
    private final AppointmentRepository apptRepo;
    private final com.example.xinqiaobackend.repository.CounselorProfileRepository counselorProfileRepository;
    private final com.example.xinqiaobackend.service.PurchaseService purchaseService;

    public ConsultProController(ScheduleSlotRepository slotRepo, 
                               AppointmentRepository apptRepo,
                               com.example.xinqiaobackend.repository.CounselorProfileRepository counselorProfileRepository,
                               com.example.xinqiaobackend.service.PurchaseService purchaseService) {
        this.slotRepo = slotRepo;
        this.apptRepo = apptRepo;
        this.counselorProfileRepository = counselorProfileRepository;
        this.purchaseService = purchaseService;
    }

    @GetMapping("/slots")
    public List<Object> slots(@RequestParam String consultantId, @RequestParam String date) {
        LocalDate d = LocalDate.parse(date);
        LocalDateTime from = d.atStartOfDay();
        LocalDateTime to = d.plusDays(1).atStartOfDay();
        DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm");
        List<ScheduleSlot> list = slotRepo.findByCounselorUsernameAndStartTimeBetween(consultantId, from, to);
        return list.stream().map(s -> {
            java.util.Map<String, Object> o = new java.util.LinkedHashMap<>();
            o.put("start", s.getStartTime().toLocalTime().format(tf));
            o.put("end", s.getEndTime().toLocalTime().format(tf));
            o.put("available", s.isAvailable());
            return o;
        }).collect(Collectors.toList());
    }

    @PostMapping("/appointments")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> request(Authentication auth,
                                     @RequestParam(required = false) String consultantId,
                                     @RequestParam(required = false) String date,
                                     @RequestParam(required = false) String time,
                                     @RequestParam(required = false) String mode,
                                     @RequestParam(required = false) String remark,
                                     @RequestBody(required = false) java.util.Map<String, Object> payload) {
        if (payload != null) {
            if (consultantId == null) consultantId = String.valueOf(payload.getOrDefault("consultantId", ""));
            if (date == null) date = String.valueOf(payload.getOrDefault("date", ""));
            if (time == null) time = String.valueOf(payload.getOrDefault("time", ""));
            if (mode == null) mode = String.valueOf(payload.getOrDefault("mode", ""));
            if (remark == null) remark = String.valueOf(payload.getOrDefault("remark", ""));
        }
        if (consultantId == null || consultantId.trim().isEmpty() || date == null || time == null) {
            java.util.Map<String, Object> res = new java.util.HashMap<>();
            res.put("ok", false);
            res.put("error", "missing params");
            return ResponseEntity.badRequest().body(res);
        }
        LocalDate d = LocalDate.parse(date);
        LocalTime t = LocalTime.parse(time);
        LocalDateTime start = LocalDateTime.of(d, t);
        List<ScheduleSlot> slots = slotRepo.findByCounselorUsernameAndStartTimeBetween(consultantId, d.atStartOfDay(), d.plusDays(1).atStartOfDay());
        ScheduleSlot matched = null;
        for (ScheduleSlot s : slots) {
            if (s.getStartTime().equals(start)) { matched = s; break; }
        }
        LocalDateTime end = matched != null ? matched.getEndTime() : start.plusMinutes(60);
        if (matched == null || !matched.isAvailable()) {
            java.util.Map<String, Object> res = new java.util.HashMap<>();
            res.put("ok", false);
            res.put("error", "no slot");
            return ResponseEntity.badRequest().body(res);
        }
        
        // 检查是否已有被批准的预约占用该时段
        List<Appointment> existingAppts = apptRepo.findByCounselorUsernameAndStartTimeLessThanAndEndTimeGreaterThan(consultantId, end, start);
        for (Appointment existing : existingAppts) {
            if (existing.getStatus() == AppointmentStatus.APPROVED) {
                java.util.Map<String, Object> res = new java.util.HashMap<>();
                res.put("ok", false);
                res.put("error", "conflict");
                return ResponseEntity.status(409).body(res);
            }
        }
        
        // 获取咨询师资料和价格
        java.util.Optional<com.example.xinqiaobackend.entity.CounselorProfile> profileOpt = counselorProfileRepository.findByUsername(consultantId);
        Double price = 0.0;
        String normalizedMode = normalizeMode(mode);
        if (profileOpt.isPresent()) {
            com.example.xinqiaobackend.entity.CounselorProfile profile = profileOpt.get();
            String finalMode = normalizedMode;
            if (finalMode == null || finalMode.trim().isEmpty()) {
                finalMode = normalizeMode(profile.getDefaultMode());
            }
            price = getPriceByMode(profile, finalMode);
        }
        
        // 创建预约，状态为PENDING，时段保持可用（允许多人预约，由咨询师选择）
        Appointment a = new Appointment();
        a.setUserUsername(auth.getName());
        a.setCounselorUsername(consultantId);
        a.setStartTime(start);
        a.setEndTime(end);
        a.setStatus(AppointmentStatus.PENDING);
        a.setMode(normalizedMode);
        a.setRemark(remark == null ? null : remark);
        a.setPrice(price);
        Appointment saved = apptRepo.save(a);
        
        // 立即从用户余额扣款（预付款）
        if (price != null && price > 0) {
            try {
                java.math.BigDecimal priceAmount = java.math.BigDecimal.valueOf(price);
                purchaseService.deductBalance(auth.getName(), saved.getId(), priceAmount);
            } catch (IllegalStateException e1) {
                // 余额不足，删除预约
                apptRepo.delete(saved);
                if ("INSUFFICIENT_BALANCE".equals(e1.getMessage())) {
                    java.util.Map<String, Object> res = new java.util.HashMap<>();
                    res.put("ok", false);
                    res.put("error", "余额不足");
                    return ResponseEntity.status(402).body(res);
                }
                throw e1;
            }
        }
        
        java.util.Map<String, Object> res = new java.util.HashMap<>();
        res.put("ok", true);
        res.put("id", String.valueOf(saved.getId()));
        return ResponseEntity.ok(res);
    }
    
    private Double getPriceByMode(com.example.xinqiaobackend.entity.CounselorProfile profile, String mode) {
        if (mode == null) return 0.0;
        
        // 将中文mode转换为英文
        String normalizedMode = normalizeMode(mode);
        
        switch (normalizedMode.toLowerCase()) {
            case "text":
                return profile.getPriceText() != null ? profile.getPriceText() : 0.0;
            case "voice":
                return profile.getPriceVoice() != null ? profile.getPriceVoice() : 0.0;
            case "video":
                return profile.getPriceVideo() != null ? profile.getPriceVideo() : 0.0;
            default:
                return profile.getPrice() != null ? profile.getPrice() : 0.0;
        }
    }
    
    private String normalizeMode(String mode) {
        if (mode == null) return "text";
        String m = mode.trim();
        // 中文到英文的映射
        if (m.contains("文字") || m.contains("文本")) return "text";
        if (m.contains("语音") || m.contains("音频")) return "voice";
        if (m.contains("视频") || m.contains("影像")) return "video";
        // 已经是英文的情况
        return m.toLowerCase();
    }
}