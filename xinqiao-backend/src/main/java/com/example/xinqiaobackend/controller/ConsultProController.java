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
                                     @RequestParam String consultantId,
                                     @RequestParam String date,
                                     @RequestParam String time,
                                     @RequestParam(required = false) String mode,
                                     @RequestParam(required = false) String remark) {
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
            return ResponseEntity.badRequest().body(java.util.Map.of("ok", false, "error", "no slot"));
        }
        boolean hasConflict = !apptRepo.findByCounselorUsernameAndStartTimeLessThanAndEndTimeGreaterThan(consultantId, end, start).isEmpty();
        if (hasConflict) return ResponseEntity.status(409).body(java.util.Map.of("ok", false, "error", "conflict"));
        Appointment a = new Appointment();
        a.setUserUsername(auth.getName());
        a.setCounselorUsername(consultantId);
        a.setStartTime(start);
        a.setEndTime(end);
        a.setStatus(AppointmentStatus.PENDING);
        Appointment saved = apptRepo.save(a);
        return ResponseEntity.ok(java.util.Map.of("ok", true, "id", String.valueOf(saved.getId())));
    }
}
