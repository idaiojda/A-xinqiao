package com.example.xinqiaobackend;

import com.example.xinqiaobackend.entity.*;
import com.example.xinqiaobackend.repository.*;
import com.example.xinqiaobackend.service.ScheduleGenerationService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@SpringBootTest
@TestPropertySource(properties = {
        "app.jwt.secret=this_is_a_very_long_test_secret_key_for_jwt_256_bits",
        "app.jwt.expiration-seconds=3600"
})
public class ScheduleGenerationServiceTests {

    @Autowired
    private ScheduleRuleRepository ruleRepo;
    @Autowired
    private ScheduleRuleExceptionRepository exRepo;
    @Autowired
    private ScheduleSlotRepository slotRepo;
    @Autowired
    private AppointmentRepository apptRepo;
    @Autowired
    private ScheduleGenerationService genService;

    @Test
    void weeklyRule_generatesSlots_skipExceptions_andConflicts() {
        String counselor = "c1";
        ScheduleRule r = new ScheduleRule();
        r.setCounselorUsername(counselor);
        r.setFrequency(ScheduleFrequency.WEEKLY);
        r.setStartDate(LocalDate.of(2025, 11, 1));
        r.setEndDate(LocalDate.of(2025, 11, 30));
        r.setStartTime(LocalTime.of(9, 0));
        r.setEndTime(LocalTime.of(10, 0));
        r.getWeekdays().add(1); // Monday
        r = ruleRepo.save(r);

        ScheduleRuleException ex = new ScheduleRuleException();
        ex.setRule(r);
        ex.setDate(LocalDate.of(2025, 11, 10)); // Monday
        exRepo.save(ex);

        Appointment a = new Appointment();
        a.setCounselorUsername(counselor);
        a.setUserUsername("u1");
        a.setStartTime(LocalDateTime.of(2025, 11, 17, 9, 0)); // Monday
        a.setEndTime(LocalDateTime.of(2025, 11, 17, 10, 0));
        a.setStatus(AppointmentStatus.APPROVED);
        apptRepo.save(a);

        int created = genService.generate(counselor, LocalDate.of(2025, 11, 1), LocalDate.of(2025, 11, 30));
        // Mondays in Nov 2025: 3rd, 10th, 17th, 24th -> expect 2 slots (skip 10th by exception, 17th by conflict)
        Assertions.assertEquals(2, created);
    }
}