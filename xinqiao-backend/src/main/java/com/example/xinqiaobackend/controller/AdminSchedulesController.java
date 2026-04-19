package com.example.xinqiaobackend.controller;

import com.example.xinqiaobackend.entity.ScheduleSlot;
import com.example.xinqiaobackend.repository.ScheduleSlotRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.web.bind.annotation.RequestBody;
import com.example.xinqiaobackend.service.ScheduleGenerationService;

@RestController
@RequestMapping("/api/admin/schedules")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSchedulesController {
    private final ScheduleSlotRepository repo;
    private final ScheduleGenerationService genService;

    public AdminSchedulesController(ScheduleSlotRepository repo, ScheduleGenerationService genService) {
        this.repo = repo;
        this.genService = genService;
    }

    @GetMapping
    public List<ScheduleSlot> list(@RequestParam String counselor,
                                   @RequestParam(required = false) String from,
                                   @RequestParam(required = false) String to,
                                   @RequestParam(required = false) Boolean available) {
        if (from != null && to != null) {
            List<ScheduleSlot> list = repo.findByCounselorUsernameAndStartTimeBetween(counselor, LocalDateTime.parse(from), LocalDateTime.parse(to));
            if (available != null) {
                java.util.List<ScheduleSlot> filtered = new java.util.ArrayList<>();
                for (ScheduleSlot s : list) if (s.isAvailable() == available) filtered.add(s);
                return filtered;
            }
            return list;
        }
        List<ScheduleSlot> list = repo.findByCounselorUsernameOrderByStartTimeAsc(counselor);
        if (available != null) {
            java.util.List<ScheduleSlot> filtered = new java.util.ArrayList<>();
            for (ScheduleSlot s : list) if (s.isAvailable() == available) filtered.add(s);
            return filtered;
        }
        return list;
    }

    @GetMapping("/export")
    public org.springframework.http.ResponseEntity<byte[]> export(@RequestParam String counselor,
                                                                  @RequestParam(required = false) String from,
                                                                  @RequestParam(required = false) String to,
                                                                  @RequestParam(required = false) Boolean available) {
        java.util.List<ScheduleSlot> slots = list(counselor, from, to, available);
        StringBuilder sb = new StringBuilder();
        sb.append("id,startTime,endTime,available\n");
        for (ScheduleSlot s : slots) {
            sb.append(s.getId()).append(',')
              .append(s.getStartTime()).append(',')
              .append(s.getEndTime()).append(',')
              .append(s.isAvailable()).append('\n');
        }
        byte[] bytes = sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=slots.csv");
        headers.set(org.springframework.http.HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8");
        return org.springframework.http.ResponseEntity.ok().headers(headers).body(bytes);
    }

    @PostMapping("/{id}/open")
    public ResponseEntity<?> open(@PathVariable Long id) {
        ScheduleSlot slot = repo.findById(id).orElse(null);
        if (slot == null) return ResponseEntity.notFound().build();
        slot.setAvailable(true);
        repo.save(slot);
        return ResponseEntity.ok(java.util.Map.of("success", true, "message", "时段已开放"));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<?> close(@PathVariable Long id) {
        ScheduleSlot slot = repo.findById(id).orElse(null);
        if (slot == null) return ResponseEntity.notFound().build();
        slot.setAvailable(false);
        repo.save(slot);
        return ResponseEntity.ok(java.util.Map.of("success", true, "message", "时段已关闭"));
    }

    @PostMapping("/generate")
    public java.util.Map<String, Object> generate(@RequestBody java.util.Map<String, String> payload) {
        java.util.Map<String, Object> res = new java.util.HashMap<>();
        String counselor = payload.getOrDefault("counselor", "");
        String from = payload.getOrDefault("from", "");
        String to = payload.getOrDefault("to", "");
        int count = 0;
        try {
            if (!counselor.isEmpty() && !from.isEmpty() && !to.isEmpty()) {
                java.time.LocalDate f = java.time.LocalDate.parse(from.substring(0, Math.min(10, from.length())));
                java.time.LocalDate t = java.time.LocalDate.parse(to.substring(0, Math.min(10, to.length())));
                count = genService.generate(counselor, f, t);
            }
            res.put("created", count);
            res.put("ok", true);
        } catch (Exception e) {
            res.put("ok", false);
            res.put("error", e.getMessage());
            res.put("created", 0);
        }
        return res;
    }

    @PostMapping("/batch")
    public java.util.Map<String, Object> batch(@RequestBody java.util.Map<String, String> payload) {
        String counselor = payload.getOrDefault("counselor", "");
        String from = payload.getOrDefault("from", "");
        String to = payload.getOrDefault("to", "");
        String action = payload.getOrDefault("action", "");
        int affected = 0;
        if (!counselor.isEmpty() && !from.isEmpty() && !to.isEmpty() && ("open".equals(action) || "close".equals(action))) {
            java.time.LocalDateTime f = java.time.LocalDateTime.parse(from);
            java.time.LocalDateTime t = java.time.LocalDateTime.parse(to);
            java.util.List<ScheduleSlot> slots = repo.findByCounselorUsernameAndStartTimeBetween(counselor, f, t);
            for (ScheduleSlot s : slots) {
                boolean target = "open".equals(action);
                if (s.isAvailable() != target) { s.setAvailable(target); repo.save(s); affected++; }
            }
        }
        java.util.Map<String, Object> res = new java.util.HashMap<>();
        res.put("affected", affected);
        return res;
    }
}
