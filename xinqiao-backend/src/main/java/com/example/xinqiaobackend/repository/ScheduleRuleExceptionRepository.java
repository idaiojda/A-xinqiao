package com.example.xinqiaobackend.repository;

import com.example.xinqiaobackend.entity.ScheduleRule;
import com.example.xinqiaobackend.entity.ScheduleRuleException;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface ScheduleRuleExceptionRepository extends JpaRepository<ScheduleRuleException, Long> {
    List<ScheduleRuleException> findByRule(ScheduleRule rule);
    List<ScheduleRuleException> findByRuleAndDateBetween(ScheduleRule rule, LocalDate from, LocalDate to);
}