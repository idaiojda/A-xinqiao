package com.example.xinqiaobackend.controller;

import com.example.xinqiaobackend.entity.Appointment;
import com.example.xinqiaobackend.entity.AppointmentStatus;
import com.example.xinqiaobackend.repository.AppointmentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import com.example.xinqiaobackend.repository.ScheduleSlotRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api")
public class AppointmentController {
    private static final Logger logger = LoggerFactory.getLogger(AppointmentController.class);
    
    private final AppointmentRepository repo;
    private final ScheduleSlotRepository slotRepo;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;
    private final com.example.xinqiaobackend.repository.NotificationRepository notificationRepository;
    private final com.example.xinqiaobackend.repository.CounselorProfileRepository counselorProfileRepository;
    private final com.example.xinqiaobackend.service.PurchaseService purchaseService;

    public AppointmentController(AppointmentRepository repo, ScheduleSlotRepository slotRepo, org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate, com.example.xinqiaobackend.repository.NotificationRepository notificationRepository, com.example.xinqiaobackend.repository.CounselorProfileRepository counselorProfileRepository, com.example.xinqiaobackend.service.PurchaseService purchaseService) {
        this.repo = repo;
        this.slotRepo = slotRepo;
        this.messagingTemplate = messagingTemplate;
        this.notificationRepository = notificationRepository;
        this.counselorProfileRepository = counselorProfileRepository;
        this.purchaseService = purchaseService;
    }

    @PostMapping("/appointments")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> request(Authentication auth,
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
        
        // 获取咨询师资料和价格
        java.util.Optional<com.example.xinqiaobackend.entity.CounselorProfile> profileOpt = counselorProfileRepository.findByUsername(counselor);
        if (!profileOpt.isPresent()) return ResponseEntity.badRequest().build();
        
        com.example.xinqiaobackend.entity.CounselorProfile profile = profileOpt.get();
        String finalMode = mode;
        if (finalMode == null || finalMode.trim().isEmpty()) {
            finalMode = profile.getDefaultMode();
        }
        
        // 根据mode获取对应的价格
        Double price = getPriceByMode(profile, finalMode);
        
        // 创建预约
        Appointment a = new Appointment();
        a.setUserUsername(auth.getName());
        a.setCounselorUsername(counselor);
        a.setStartTime(s);
        a.setEndTime(e);
        a.setStatus(AppointmentStatus.PENDING);
        a.setMode(finalMode);
        a.setPrice(price);
        a = repo.save(a);
        
        // 立即从用户余额扣款（预付款）
        if (price != null && price > 0) {
            try {
                java.math.BigDecimal priceAmount = java.math.BigDecimal.valueOf(price);
                purchaseService.deductBalance(auth.getName(), a.getId(), priceAmount);
            } catch (IllegalStateException e1) {
                // 余额不足，删除预约
                repo.delete(a);
                if ("INSUFFICIENT_BALANCE".equals(e1.getMessage())) {
                    return ResponseEntity.status(402).body(java.util.Collections.singletonMap("error", "余额不足"));
                }
                throw e1;
            }
        }
        
        return ResponseEntity.ok(a);
    }
    
    private Double getPriceByMode(com.example.xinqiaobackend.entity.CounselorProfile profile, String mode) {
        if (mode == null) return 0.0;
        switch (mode.toLowerCase()) {
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
        
        // 批准预约时，将预付款分配给咨询师和平台
        if (a.getPrice() != null && a.getPrice() > 0) {
            java.math.BigDecimal price = java.math.BigDecimal.valueOf(a.getPrice());
            purchaseService.recordPurchase(a.getUserUsername(), a.getId(), price);
            purchaseService.distributeRevenue(a.getCounselorUsername(), a.getUserUsername(), a.getId(), price);
        }
        
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
        return ResponseEntity.ok(java.util.Map.of("success", true, "message", "预约已批准"));
    }

    @PostMapping("/counselor/appointments/{id}/reject")
    @PreAuthorize("hasRole('COUNSELOR')")
    public ResponseEntity<?> reject(Authentication auth, @PathVariable Long id) {
        Appointment a = repo.findById(id).orElse(null);
        if (a == null) return ResponseEntity.notFound().build();
        if (!a.getCounselorUsername().equals(auth.getName())) return ResponseEntity.status(403).build();
        
        // 拒绝预约时，退款给用户
        if (a.getPrice() != null && a.getPrice() > 0) {
            java.math.BigDecimal price = java.math.BigDecimal.valueOf(a.getPrice());
            purchaseService.refundBalance(a.getUserUsername(), price);
        }
        
        a.setStatus(AppointmentStatus.REJECTED);
        repo.save(a);
        
        // 拒绝预约时，时段保持可用（因为创建预约时没有关闭时段）
        
        messagingTemplate.convertAndSend("/topic/appointments/" + a.getUserUsername(), java.util.Collections.singletonMap("status", a.getStatus().name()));
        com.example.xinqiaobackend.entity.Notification n = new com.example.xinqiaobackend.entity.Notification();
        n.setUsername(a.getUserUsername());
        n.setType("appointment");
        n.setTitle("预约被拒绝");
        n.setContent("您的预约未通过，已退款至余额");
        notificationRepository.save(n);
        return ResponseEntity.ok(java.util.Map.of("success", true, "message", "预约已拒绝"));
    }

    @PostMapping("/counselor/appointments/{id}/complete")
    @PreAuthorize("hasRole('COUNSELOR')")
    public ResponseEntity<?> complete(Authentication auth, @PathVariable Long id) {
        Appointment a = repo.findById(id).orElse(null);
        if (a == null) return ResponseEntity.notFound().build();
        if (!a.getCounselorUsername().equals(auth.getName())) return ResponseEntity.status(403).build();
        
        // 完成预约（款项已在批准时分配，这里只更新状态）
        a.setStatus(AppointmentStatus.COMPLETED);
        repo.save(a);
        messagingTemplate.convertAndSend("/topic/appointments/" + a.getUserUsername(), java.util.Collections.singletonMap("status", a.getStatus().name()));
        com.example.xinqiaobackend.entity.Notification n = new com.example.xinqiaobackend.entity.Notification();
        n.setUsername(a.getUserUsername());
        n.setType("appointment");
        n.setTitle("预约已完成");
        n.setContent("感谢您的使用，欢迎评价与反馈");
        notificationRepository.save(n);
        return ResponseEntity.ok(java.util.Map.of("success", true, "message", "预约已完成"));
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
        logger.info("用户 {} 请求取消预约 {}", auth.getName(), id);
        
        Appointment a = repo.findById(id).orElse(null);
        if (a == null) {
            logger.warn("预约不存在: {}", id);
            return ResponseEntity.status(404).body(java.util.Map.of("error", "预约不存在"));
        }
        
        if (!a.getUserUsername().equals(auth.getName())) {
            logger.warn("用户 {} 无权取消预约 {}（所有者: {}）", auth.getName(), id, a.getUserUsername());
            return ResponseEntity.status(403).body(java.util.Map.of("error", "无权操作此预约"));
        }
        
        if (a.getStatus() != AppointmentStatus.PENDING && a.getStatus() != AppointmentStatus.APPROVED) {
            logger.warn("预约 {} 状态不允许取消: {}", id, a.getStatus());
            return ResponseEntity.status(400).body(java.util.Map.of("error", "预约状态不允许取消", "status", a.getStatus().toString()));
        }
        
        if (a.getStartTime().isBefore(java.time.LocalDateTime.now())) {
            logger.warn("预约 {} 已过期，无法取消。预约时间: {}, 当前时间: {}", id, a.getStartTime(), java.time.LocalDateTime.now());
            return ResponseEntity.status(400).body(java.util.Map.of("error", "预约已过期，无法取消", "startTime", a.getStartTime().toString()));
        }
        
        // 如果预约已被批准，需要恢复时段可用性
        boolean wasApproved = a.getStatus() == AppointmentStatus.APPROVED;
        
        // 取消预约时退款
        if (a.getPrice() != null && a.getPrice() > 0) {
            java.math.BigDecimal price = java.math.BigDecimal.valueOf(a.getPrice());
            
            if (a.getStatus() == AppointmentStatus.PENDING) {
                // PENDING状态：款项还在用户账户的"冻结"状态，直接退回
                logger.info("退款给用户 {}，金额: {}", a.getUserUsername(), price);
                purchaseService.refundBalance(a.getUserUsername(), price);
            } else if (wasApproved) {
                // APPROVED状态：款项已分配给咨询师和平台，需要撤销分配并退款
                logger.info("撤销分配并退款给用户 {}，金额: {}", a.getUserUsername(), price);
                purchaseService.reverseDistribution(
                    a.getCounselorUsername(), 
                    a.getUserUsername(), 
                    a.getId(), 
                    price
                );
            }
        }
        
        a.setStatus(AppointmentStatus.CANCELLED);
        repo.save(a);
        logger.info("预约 {} 已取消", id);
        
        if (wasApproved) {
            // 只有已批准的预约取消时才需要恢复时段
            java.time.LocalDate d = a.getStartTime().toLocalDate();
            java.util.List<com.example.xinqiaobackend.entity.ScheduleSlot> daySlots = slotRepo.findByCounselorUsernameAndStartTimeBetween(a.getCounselorUsername(), d.atStartOfDay(), d.plusDays(1).atStartOfDay());
            for (com.example.xinqiaobackend.entity.ScheduleSlot s : daySlots) {
                if (s.getStartTime().equals(a.getStartTime())) {
                    s.setAvailable(true);
                    slotRepo.save(s);
                    logger.info("恢复时段可用性: {}", s.getStartTime());
                    break;
                }
            }
        }
        
        com.example.xinqiaobackend.entity.Notification n = new com.example.xinqiaobackend.entity.Notification();
        n.setUsername(a.getCounselorUsername());
        n.setType("appointment");
        n.setTitle("用户取消预约");
        n.setContent("用户已取消预约，时间：" + a.getStartTime() + (wasApproved ? "（已退款）" : ""));
        notificationRepository.save(n);
        
        return ResponseEntity.ok(java.util.Map.of("success", true, "message", "取消成功"));
    }

    @PostMapping("/appointments/{id}/reschedule")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> reschedule(Authentication auth, @PathVariable Long id,
                                        @RequestParam String date,
                                        @RequestParam String time) {
        logger.info("用户 {} 请求改期预约 {} 到 {} {}", auth.getName(), id, date, time);
        
        Appointment a = repo.findById(id).orElse(null);
        if (a == null) {
            logger.warn("预约不存在: {}", id);
            return ResponseEntity.notFound().build();
        }
        
        if (!a.getUserUsername().equals(auth.getName())) {
            logger.warn("用户 {} 无权改期预约 {}（所有者: {}）", auth.getName(), id, a.getUserUsername());
            return ResponseEntity.status(403).build();
        }
        
        if (a.getStartTime().isBefore(java.time.LocalDateTime.now())) {
            logger.warn("预约 {} 已过期，无法改期", id);
            return ResponseEntity.badRequest().build();
        }
        
        java.time.LocalDate d = java.time.LocalDate.parse(date);
        java.time.LocalTime t = java.time.LocalTime.parse(time);
        java.time.LocalDateTime newStart = java.time.LocalDateTime.of(d, t);
        java.time.LocalDateTime newEnd = a.getEndTime();
        
        java.util.List<com.example.xinqiaobackend.entity.ScheduleSlot> daySlots = slotRepo.findByCounselorUsernameAndStartTimeBetween(a.getCounselorUsername(), d.atStartOfDay(), d.plusDays(1).atStartOfDay());
        com.example.xinqiaobackend.entity.ScheduleSlot matched = null;
        for (com.example.xinqiaobackend.entity.ScheduleSlot s : daySlots) {
            if (s.getStartTime().equals(newStart)) { 
                matched = s; 
                break; 
            }
        }
        
        if (matched == null || !matched.isAvailable()) {
            logger.warn("时段不可用: {} {}", date, time);
            return ResponseEntity.badRequest().build();
        }
        
        boolean conflict = !repo.findByCounselorUsernameAndStartTimeLessThanAndEndTimeGreaterThan(a.getCounselorUsername(), matched.getEndTime(), newStart).isEmpty();
        if (conflict) {
            logger.warn("时段冲突: {} {}", date, time);
            return ResponseEntity.status(409).build();
        }
        
        a.setStartTime(newStart);
        a.setEndTime(matched.getEndTime());
        if (a.getStatus() == AppointmentStatus.APPROVED) {
            a.setStatus(AppointmentStatus.PENDING);
            logger.info("预约 {} 状态从 APPROVED 改为 PENDING，需要咨询师重新批准", id);
        }
        repo.save(a);
        logger.info("预约 {} 改期成功，新时间: {}", id, newStart);
        
        com.example.xinqiaobackend.entity.Notification n = new com.example.xinqiaobackend.entity.Notification();
        n.setUsername(a.getCounselorUsername());
        n.setType("appointment");
        n.setTitle("用户改期申请");
        n.setContent("用户申请改期至：" + a.getStartTime());
        notificationRepository.save(n);
        
        return ResponseEntity.ok(java.util.Map.of("success", true, "message", "改期成功"));
    }
}