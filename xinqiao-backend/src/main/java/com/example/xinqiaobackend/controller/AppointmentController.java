package com.example.xinqiaobackend.controller;

import com.example.xinqiaobackend.entity.Appointment;
import com.example.xinqiaobackend.entity.AppointmentStatus;
import com.example.xinqiaobackend.repository.AppointmentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import com.example.xinqiaobackend.repository.ScheduleSlotRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api")
public class AppointmentController {
    private final AppointmentRepository repo;
    private final ScheduleSlotRepository slotRepo;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;
    private final com.example.xinqiaobackend.repository.NotificationRepository notificationRepository;
    private final com.example.xinqiaobackend.repository.CounselorProfileRepository counselorProfileRepository;

    public AppointmentController(AppointmentRepository repo, ScheduleSlotRepository slotRepo, org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate, com.example.xinqiaobackend.repository.NotificationRepository notificationRepository, com.example.xinqiaobackend.repository.CounselorProfileRepository counselorProfileRepository) {
        this.repo = repo;
        this.slotRepo = slotRepo;
        this.messagingTemplate = messagingTemplate;
        this.notificationRepository = notificationRepository;
        this.counselorProfileRepository = counselorProfileRepository;
    }

    @PostMapping("/appointments")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Appointment> request(Authentication auth,
                                               @RequestParam String counselor,
                                               @RequestParam String start,
                                               @RequestParam String end,
                                               @RequestParam(required = false) String mode) {
        LocalDateTime s = LocalDateTime.parse(start);
        LocalDateTime e = LocalDateTime.parse(end);
        boolean hasSlot = !slotRepo
                .findByCounselorUsernameAndStartTimeLessThanEqualAndEndTimeGreaterThanEqualAndAvailableTrue(counselor, s, e)
                .isEmpty();
        if (!hasSlot) return ResponseEntity.badRequest().build();

        boolean hasConflict = !repo
                .findByCounselorUsernameAndStartTimeLessThanAndEndTimeGreaterThan(counselor, e, s)
                .isEmpty();
        if (hasConflict) return ResponseEntity.status(409).build();
        Appointment a = new Appointment();
        a.setUserUsername(auth.getName());
        a.setCounselorUsername(counselor);
        a.setStartTime(s);
        a.setEndTime(e);
        a.setStatus(AppointmentStatus.PENDING);
        String finalMode = mode;
        if (finalMode == null || finalMode.trim().isEmpty()) {
            java.util.Optional<com.example.xinqiaobackend.entity.CounselorProfile> pf = counselorProfileRepository.findByUsername(counselor);
            if (pf.isPresent()) finalMode = pf.get().getDefaultMode();
        }
        a.setMode(finalMode);
        return ResponseEntity.ok(repo.save(a));
    }

    @GetMapping("/counselor/appointments")
    @PreAuthorize("hasRole('COUNSELOR')")
    public List<Appointment> counselorList(Authentication auth,
                                           @RequestParam(required = false) String status) {
        if (status != null && !status.isEmpty()) {
            try {
                AppointmentStatus st = AppointmentStatus.valueOf(status);
                return repo.findByCounselorUsernameAndStatus(auth.getName(), st);
            } catch (Exception ignored) { }
        }
        return repo.findByCounselorUsernameOrderByStartTimeAsc(auth.getName());
    }

    @GetMapping("/appointments/mine")
    @PreAuthorize("hasRole('USER')")
    public List<Appointment> userList(Authentication auth,
                                      @RequestParam(required = false) String status) {
        if (status != null && !status.isEmpty()) {
            try {
                AppointmentStatus st = AppointmentStatus.valueOf(status);
                java.util.List<Appointment> list = repo.findByUserUsernameOrderByStartTimeAsc(auth.getName());
                return list.stream().filter(a -> a.getStatus() == st).collect(java.util.stream.Collectors.toList());
            } catch (Exception ignored) { }
        }
        return repo.findByUserUsernameOrderByStartTimeAsc(auth.getName());
    }

    @PostMapping("/counselor/appointments/{id}/approve")
    @PreAuthorize("hasRole('COUNSELOR')")
    public ResponseEntity<?> approve(Authentication auth, @PathVariable Long id) {
        Appointment a = repo.findById(id).orElse(null);
        if (a == null) return ResponseEntity.notFound().build();
        if (!a.getCounselorUsername().equals(auth.getName())) return ResponseEntity.status(403).build();
        a.setStatus(AppointmentStatus.APPROVED);
        repo.save(a);
        slotRepo
                .findByCounselorUsernameAndStartTimeLessThanEqualAndEndTimeGreaterThanEqualAndAvailableTrue(a.getCounselorUsername(), a.getStartTime(), a.getEndTime())
                .forEach(slot -> { slot.setAvailable(false); slotRepo.save(slot); });
        messagingTemplate.convertAndSend("/topic/appointments/" + a.getUserUsername(), java.util.Collections.singletonMap("status", a.getStatus().name()));
        com.example.xinqiaobackend.entity.Notification n = new com.example.xinqiaobackend.entity.Notification();
        n.setUsername(a.getUserUsername());
        n.setType("appointment");
        n.setTitle("预约已通过");
        n.setContent("您的预约已通过，时间：" + a.getStartTime());
        notificationRepository.save(n);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/counselor/appointments/{id}/reject")
    @PreAuthorize("hasRole('COUNSELOR')")
    public ResponseEntity<?> reject(Authentication auth, @PathVariable Long id) {
        Appointment a = repo.findById(id).orElse(null);
        if (a == null) return ResponseEntity.notFound().build();
        if (!a.getCounselorUsername().equals(auth.getName())) return ResponseEntity.status(403).build();
        a.setStatus(AppointmentStatus.REJECTED);
        repo.save(a);
        messagingTemplate.convertAndSend("/topic/appointments/" + a.getUserUsername(), java.util.Collections.singletonMap("status", a.getStatus().name()));
        com.example.xinqiaobackend.entity.Notification n = new com.example.xinqiaobackend.entity.Notification();
        n.setUsername(a.getUserUsername());
        n.setType("appointment");
        n.setTitle("预约被拒绝");
        n.setContent("您的预约未通过，请重新选择时段");
        notificationRepository.save(n);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/counselor/appointments/{id}/complete")
    @PreAuthorize("hasRole('COUNSELOR')")
    public ResponseEntity<?> complete(Authentication auth, @PathVariable Long id) {
        Appointment a = repo.findById(id).orElse(null);
        if (a == null) return ResponseEntity.notFound().build();
        if (!a.getCounselorUsername().equals(auth.getName())) return ResponseEntity.status(403).build();
        a.setStatus(AppointmentStatus.COMPLETED);
        repo.save(a);
        messagingTemplate.convertAndSend("/topic/appointments/" + a.getUserUsername(), java.util.Collections.singletonMap("status", a.getStatus().name()));
        com.example.xinqiaobackend.entity.Notification n = new com.example.xinqiaobackend.entity.Notification();
        n.setUsername(a.getUserUsername());
        n.setType("appointment");
        n.setTitle("预约已完成");
        n.setContent("感谢您的使用，欢迎评价与反馈");
        notificationRepository.save(n);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/appointments/{id}")
    public ResponseEntity<Appointment> detail(Authentication auth, @PathVariable Long id) {
        Appointment a = repo.findById(id).orElse(null);
        if (a == null) return ResponseEntity.notFound().build();
        String user = auth.getName();
        boolean isOwner = user.equals(a.getUserUsername()) || user.equals(a.getCounselorUsername());
        if (!isOwner) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(a);
    }

    @PostMapping("/appointments/{id}/cancel")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> cancel(Authentication auth, @PathVariable Long id) {
        Appointment a = repo.findById(id).orElse(null);
        if (a == null) return ResponseEntity.notFound().build();
        if (!a.getUserUsername().equals(auth.getName())) return ResponseEntity.status(403).build();
        if (a.getStatus() != AppointmentStatus.PENDING && a.getStatus() != AppointmentStatus.APPROVED) return ResponseEntity.badRequest().build();
        if (a.getStartTime().isBefore(java.time.LocalDateTime.now())) return ResponseEntity.badRequest().build();
        a.setStatus(AppointmentStatus.CANCELLED);
        repo.save(a);
        java.time.LocalDate d = a.getStartTime().toLocalDate();
        java.util.List<com.example.xinqiaobackend.entity.ScheduleSlot> daySlots = slotRepo.findByCounselorUsernameAndStartTimeBetween(a.getCounselorUsername(), d.atStartOfDay(), d.plusDays(1).atStartOfDay());
        for (com.example.xinqiaobackend.entity.ScheduleSlot s : daySlots) {
            if (s.getStartTime().equals(a.getStartTime())) {
                s.setAvailable(true);
                slotRepo.save(s);
                break;
            }
        }
        com.example.xinqiaobackend.entity.Notification n = new com.example.xinqiaobackend.entity.Notification();
        n.setUsername(a.getCounselorUsername());
        n.setType("appointment");
        n.setTitle("用户取消预约");
        n.setContent("用户已取消预约，时间：" + a.getStartTime());
        notificationRepository.save(n);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/appointments/{id}/reschedule")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> reschedule(Authentication auth, @PathVariable Long id,
                                        @RequestParam String date,
                                        @RequestParam String time) {
        Appointment a = repo.findById(id).orElse(null);
        if (a == null) return ResponseEntity.notFound().build();
        if (!a.getUserUsername().equals(auth.getName())) return ResponseEntity.status(403).build();
        if (a.getStartTime().isBefore(java.time.LocalDateTime.now())) return ResponseEntity.badRequest().build();
        java.time.LocalDate d = java.time.LocalDate.parse(date);
        java.time.LocalTime t = java.time.LocalTime.parse(time);
        java.time.LocalDateTime newStart = java.time.LocalDateTime.of(d, t);
        java.time.LocalDateTime newEnd = a.getEndTime();
        java.util.List<com.example.xinqiaobackend.entity.ScheduleSlot> daySlots = slotRepo.findByCounselorUsernameAndStartTimeBetween(a.getCounselorUsername(), d.atStartOfDay(), d.plusDays(1).atStartOfDay());
        com.example.xinqiaobackend.entity.ScheduleSlot matched = null;
        for (com.example.xinqiaobackend.entity.ScheduleSlot s : daySlots) {
            if (s.getStartTime().equals(newStart)) { matched = s; break; }
        }
        if (matched == null || !matched.isAvailable()) return ResponseEntity.badRequest().build();
        boolean conflict = !repo.findByCounselorUsernameAndStartTimeLessThanAndEndTimeGreaterThan(a.getCounselorUsername(), matched.getEndTime(), newStart).isEmpty();
        if (conflict) return ResponseEntity.status(409).build();
        a.setStartTime(newStart);
        a.setEndTime(matched.getEndTime());
        if (a.getStatus() == AppointmentStatus.APPROVED) a.setStatus(AppointmentStatus.PENDING);
        repo.save(a);
        com.example.xinqiaobackend.entity.Notification n = new com.example.xinqiaobackend.entity.Notification();
        n.setUsername(a.getCounselorUsername());
        n.setType("appointment");
        n.setTitle("用户改期申请");
        n.setContent("用户申请改期至：" + a.getStartTime());
        notificationRepository.save(n);
        return ResponseEntity.ok().build();
    }
}