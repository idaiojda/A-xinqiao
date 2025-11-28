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

    public ConsultProController(ScheduleSlotRepository slotRepo, AppointmentRepository apptRepo) {
        this.slotRepo = slotRepo;
        this.apptRepo = apptRepo;
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
        boolean hasConflict = !apptRepo.findByCounselorUsernameAndStartTimeLessThanAndEndTimeGreaterThan(consultantId, end, start).isEmpty();
        if (hasConflict) {
            java.util.Map<String, Object> res = new java.util.HashMap<>();
            res.put("ok", false);
            res.put("error", "conflict");
            return ResponseEntity.status(409).body(res);
        }
        Appointment a = new Appointment();
        a.setUserUsername(auth.getName());
        a.setCounselorUsername(consultantId);
        a.setStartTime(start);
        a.setEndTime(end);
        a.setStatus(AppointmentStatus.PENDING);
        a.setMode(mode == null ? null : mode);
        a.setRemark(remark == null ? null : remark);
        Appointment saved = apptRepo.save(a);
        java.util.Map<String, Object> res = new java.util.HashMap<>();
        res.put("ok", true);
        res.put("id", String.valueOf(saved.getId()));
        return ResponseEntity.ok(res);
    }
}