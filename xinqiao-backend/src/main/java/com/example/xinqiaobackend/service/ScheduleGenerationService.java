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
        List<ScheduleRule> rules = ruleRepo.findByCounselorUsernameAndStartDateLessThanEqualAndEndDateGreaterThanEqual(counselorUsername, to, from);
        int created = 0;
        for (ScheduleRule r : rules) {
            LocalDate start = from.isBefore(r.getStartDate()) ? r.getStartDate() : from;
            LocalDate end = to.isAfter(r.getEndDate()) ? r.getEndDate() : to;
            Set<LocalDate> exclude = new HashSet<>();
            for (LocalDate d : readExceptions(r)) {
                if (!d.isBefore(start) && !d.isAfter(end)) exclude.add(d);
            }
            LocalDate cur = start;
            while (!cur.isAfter(end)) {
                if (!exclude.contains(cur)) {
                    boolean match = r.getFrequency() == ScheduleFrequency.DAILY ||
                            (r.getFrequency() == ScheduleFrequency.WEEKLY && readWeekdays(r).contains(cur.getDayOfWeek().getValue()));
                    if (match) {
                        LocalDateTime s = LocalDateTime.of(cur, r.getStartTime());
                        LocalDateTime e = LocalDateTime.of(cur, r.getEndTime());
                        boolean slotConflict = slotRepo.findByCounselorUsernameOrderByStartTimeAsc(counselorUsername)
                                .stream().anyMatch(sl -> sl.getStartTime().isBefore(e) && sl.getEndTime().isAfter(s));
                        boolean apptConflict = !apptRepo.findByCounselorUsernameAndStartTimeLessThanAndEndTimeGreaterThan(counselorUsername, e, s).isEmpty();
                        if (!slotConflict && !apptConflict) {
                            ScheduleSlot slot = new ScheduleSlot();
                            slot.setCounselorUsername(counselorUsername);
                            slot.setStartTime(s);
                            slot.setEndTime(e);
                            slot.setAvailable(true);
                            slotRepo.save(slot);
                            created++;
                        }
                    }
                }
                cur = cur.plusDays(1);
            }
        }
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
