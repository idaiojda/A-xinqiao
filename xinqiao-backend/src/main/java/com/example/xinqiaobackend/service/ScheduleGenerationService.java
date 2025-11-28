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
    private final ScheduleRuleExceptionRepository exRepo;
    private final ScheduleSlotRepository slotRepo;
    private final AppointmentRepository apptRepo;

    public ScheduleGenerationService(ScheduleRuleRepository ruleRepo,
                                     ScheduleRuleExceptionRepository exRepo,
                                     ScheduleSlotRepository slotRepo,
                                     AppointmentRepository apptRepo) {
        this.ruleRepo = ruleRepo;
        this.exRepo = exRepo;
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
            exRepo.findByRuleAndDateBetween(r, start, end).forEach(e -> exclude.add(e.getDate()));
            LocalDate cur = start;
            while (!cur.isAfter(end)) {
                if (!exclude.contains(cur)) {
                    boolean match = r.getFrequency() == ScheduleFrequency.DAILY ||
                            (r.getFrequency() == ScheduleFrequency.WEEKLY && r.getWeekdays().contains(cur.getDayOfWeek().getValue()));
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
}