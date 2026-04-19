package com.example.xinqiaobackend.controller;

import com.example.xinqiaobackend.entity.ScheduleSlot;
import com.example.xinqiaobackend.repository.ScheduleSlotRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/counselor/schedule")
@PreAuthorize("hasRole('COUNSELOR')")
public class CounselorScheduleController {
    private final ScheduleSlotRepository repo;

    public CounselorScheduleController(ScheduleSlotRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<ScheduleSlot> mySlots(Authentication auth,
                                      @RequestParam(required = false) String from,
                                      @RequestParam(required = false) String to) {
        String counselor = auth.getName();
        if (from != null && to != null) {
            return repo.findByCounselorUsernameAndStartTimeBetween(counselor, java.time.LocalDateTime.parse(from), java.time.LocalDateTime.parse(to));
        }
        return repo.findByCounselorUsernameOrderByStartTimeAsc(counselor);
    }

    @PostMapping
    public ResponseEntity<ScheduleSlot> create(Authentication auth,
                                               @RequestParam String start,
                                               @RequestParam String end) {
        String counselor = auth.getName();
        LocalDateTime s = LocalDateTime.parse(start);
        LocalDateTime e = LocalDateTime.parse(end);
        ScheduleSlot slot = new ScheduleSlot();
        slot.setCounselorUsername(counselor);
        slot.setStartTime(s);
        slot.setEndTime(e);
        slot.setAvailable(true);
        return ResponseEntity.ok(repo.save(slot));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<?> close(Authentication auth, @PathVariable Long id) {
        ScheduleSlot slot = repo.findById(id).orElse(null);
        if (slot == null) return ResponseEntity.notFound().build();
        if (!slot.getCounselorUsername().equals(auth.getName())) return ResponseEntity.status(403).build();
        slot.setAvailable(false);
        repo.save(slot);
        return ResponseEntity.ok(java.util.Map.of("success", true, "message", "时段已关闭"));
    }

    @PostMapping("/{id}/open")
    public ResponseEntity<?> open(Authentication auth, @PathVariable Long id) {
        ScheduleSlot slot = repo.findById(id).orElse(null);
        if (slot == null) return ResponseEntity.notFound().build();
        if (!slot.getCounselorUsername().equals(auth.getName())) return ResponseEntity.status(403).build();
        slot.setAvailable(true);
        repo.save(slot);
        return ResponseEntity.ok(java.util.Map.of("success", true, "message", "时段已开放"));
    }
}