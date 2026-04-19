package com.example.xinqiaobackend.service;

import com.example.xinqiaobackend.entity.*;
import com.example.xinqiaobackend.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ScheduleGenerationService {
    private final ScheduleRuleRepository ruleRepo;
    private final ScheduleSlotRepository slotRepo;
    private final AppointmentRepository apptRepo;

    public ScheduleGenerationService(ScheduleRuleRepository ruleRepo,
                                     ScheduleSlotRepository slotRepo,
                                     AppointmentRepository apptRepo) {
        this.ruleRepo = ruleRepo;
        this.slotRepo = slotRepo;
        this.apptRepo = apptRepo;
    }

    public int generate(String counselorUsername, LocalDate from, LocalDate to) {
        System.out.println("=== 开始生成时段 ===");
        System.out.println("咨询师: " + counselorUsername);
        System.out.println("日期范围: " + from + " 至 " + to);
        
        List<ScheduleRule> rules = ruleRepo.findByCounselorUsernameAndStartDateLessThanEqualAndEndDateGreaterThanEqual(counselorUsername, to, from);
        System.out.println("找到规则数量: " + rules.size());
        
        int created = 0;
        for (ScheduleRule r : rules) {
            System.out.println("处理规则 ID: " + r.getId());
            System.out.println("  频率: " + r.getFrequency());
            System.out.println("  规则日期: " + r.getStartDate() + " 至 " + r.getEndDate());
            System.out.println("  工作时间: " + r.getStartTime() + " - " + r.getEndTime());
            
            LocalDate start = from.isBefore(r.getStartDate()) ? r.getStartDate() : from;
            LocalDate end = to.isAfter(r.getEndDate()) ? r.getEndDate() : to;
            System.out.println("  实际生成范围: " + start + " 至 " + end);
            
            Set<LocalDate> exclude = new HashSet<>();
            for (LocalDate d : readExceptions(r)) {
                if (!d.isBefore(start) && !d.isAfter(end)) exclude.add(d);
            }
            System.out.println("  排除日期数量: " + exclude.size());
            
            LocalDate cur = start;
            while (!cur.isAfter(end)) {
                if (!exclude.contains(cur)) {
                    boolean match = r.getFrequency() == ScheduleFrequency.DAILY ||
                            (r.getFrequency() == ScheduleFrequency.WEEKLY && readWeekdays(r).contains(cur.getDayOfWeek().getValue()));
                    
                    if (match) {
                        LocalDateTime s = LocalDateTime.of(cur, r.getStartTime());
                        LocalDateTime e = LocalDateTime.of(cur, r.getEndTime());
                        
                        // 将大时段拆分为每60分钟的小时段
                        LocalDateTime slotStart = s;
                        while (slotStart.isBefore(e)) {
                            LocalDateTime slotEnd = slotStart.plusMinutes(60);
                            if (slotEnd.isAfter(e)) {
                                slotEnd = e; // 最后一个时段可能不足60分钟
                            }
                            
                            // 使用final变量供lambda使用
                            final LocalDateTime finalSlotStart = slotStart;
                            final LocalDateTime finalSlotEnd = slotEnd;
                            
                            boolean slotConflict = slotRepo.findByCounselorUsernameOrderByStartTimeAsc(counselorUsername)
                                    .stream().anyMatch(sl -> sl.getStartTime().isBefore(finalSlotEnd) && sl.getEndTime().isAfter(finalSlotStart));
                            
                            // 只检查已批准的预约冲突，PENDING状态的预约不算冲突
                            List<Appointment> conflictingAppts = apptRepo.findByCounselorUsernameAndStartTimeLessThanAndEndTimeGreaterThan(counselorUsername, finalSlotEnd, finalSlotStart);
                            boolean apptConflict = false;
                            if (!conflictingAppts.isEmpty()) {
                                System.out.println("  检查 " + cur + " " + finalSlotStart.toLocalTime() + " 的预约冲突:");
                                for (Appointment appt : conflictingAppts) {
                                    System.out.println("    - 预约ID: " + appt.getId() + 
                                                     ", 用户: " + appt.getUserUsername() + 
                                                     ", 时间: " + appt.getStartTime() + " - " + appt.getEndTime() + 
                                                     ", 状态: " + appt.getStatus());
                                    if (appt.getStatus() == AppointmentStatus.APPROVED) {
                                        apptConflict = true;
                                    }
                                }
                            }
                            
                            if (slotConflict) {
                                System.out.println("  跳过 " + cur + " " + finalSlotStart.toLocalTime() + ": 时段冲突");
                            } else if (apptConflict) {
                                System.out.println("  跳过 " + cur + " " + finalSlotStart.toLocalTime() + ": 已批准的预约冲突");
                            } else {
                                if (!conflictingAppts.isEmpty()) {
                                    System.out.println("  注意 " + cur + " " + finalSlotStart.toLocalTime() + ": 有 " + conflictingAppts.size() + " 个待审核预约，但仍然创建时段");
                                }
                                ScheduleSlot slot = new ScheduleSlot();
                                slot.setCounselorUsername(counselorUsername);
                                slot.setStartTime(finalSlotStart);
                                slot.setEndTime(finalSlotEnd);
                                slot.setAvailable(true);
                                slotRepo.save(slot);
                                created++;
                                System.out.println("  ✓ 创建时段: " + finalSlotStart + " - " + finalSlotEnd);
                            }
                            
                            slotStart = slotEnd; // 移动到下一个时段
                        }
                    } else {
                        System.out.println("  跳过 " + cur + ": 不匹配频率规则");
                    }
                }
                cur = cur.plusDays(1);
            }
        }
        
        System.out.println("=== 生成完成，共创建 " + created + " 个时段 ===");
        return created;
    }

    private java.util.List<Integer> readWeekdays(ScheduleRule r) {
        try {
            String cfg = r.getConfig();
            if (cfg == null || cfg.isEmpty()) return java.util.Collections.emptyList();
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            ScheduleRuleConfig c = om.readValue(cfg, ScheduleRuleConfig.class);
            return c.getWeekdays() != null ? c.getWeekdays() : java.util.Collections.emptyList();
        } catch (Exception ignored) {
            return java.util.Collections.emptyList();
        }
    }

    private java.util.List<LocalDate> readExceptions(ScheduleRule r) {
        try {
            String cfg = r.getConfig();
            if (cfg == null || cfg.isEmpty()) return java.util.Collections.emptyList();
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            ScheduleRuleConfig c = om.readValue(cfg, ScheduleRuleConfig.class);
            java.util.List<String> xs = c.getExceptions();
            java.util.List<LocalDate> out = new java.util.ArrayList<>();
            if (xs != null) {
                for (String s : xs) {
                    try { out.add(LocalDate.parse(s)); } catch (Exception ignored) {}
                }
            }
            return out;
        } catch (Exception ignored) {
            return java.util.Collections.emptyList();
        }
    }
}
