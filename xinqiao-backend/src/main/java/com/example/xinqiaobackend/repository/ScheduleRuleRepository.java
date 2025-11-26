package com.example.xinqiaobackend.repository;

import com.example.xinqiaobackend.entity.ScheduleRule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface ScheduleRuleRepository extends JpaRepository<ScheduleRule, Long> {
    List<ScheduleRule> findByCounselorUsernameAndStartDateLessThanEqualAndEndDateGreaterThanEqual(String counselorUsername, LocalDate from, LocalDate to);
    List<ScheduleRule> findByCounselorUsernameOrderByStartDateAsc(String counselorUsername);
}