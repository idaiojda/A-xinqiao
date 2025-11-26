package com.example.xinqiaobackend.controller;

import com.example.xinqiaobackend.entity.ScheduleFrequency;
import com.example.xinqiaobackend.entity.ScheduleRule;
import com.example.xinqiaobackend.entity.ScheduleRuleException;
import com.example.xinqiaobackend.repository.ScheduleRuleExceptionRepository;
import com.example.xinqiaobackend.repository.ScheduleRuleRepository;
import com.example.xinqiaobackend.service.ScheduleGenerationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/counselor/schedule/rules")
@PreAuthorize("hasRole('COUNSELOR')")
public class CounselorScheduleRulesController {
    private final ScheduleRuleRepository ruleRepo;
    private final ScheduleRuleExceptionRepository exRepo;
    private final ScheduleGenerationService genService;

    public CounselorScheduleRulesController(ScheduleRuleRepository ruleRepo,
                                            ScheduleRuleExceptionRepository exRepo,
                                            ScheduleGenerationService genService) {
        this.ruleRepo = ruleRepo;
        this.exRepo = exRepo;
        this.genService = genService;
    }

    @GetMapping
    public List<ScheduleRule> list(Authentication auth) {
        return ruleRepo.findByCounselorUsernameOrderByStartDateAsc(auth.getName());
    }

    @PostMapping
    public ResponseEntity<ScheduleRule> create(Authentication auth,
                                               @RequestParam String frequency,
                                               @RequestParam String startDate,
                                               @RequestParam String endDate,
                                               @RequestParam String startTime,
                                               @RequestParam String endTime,
                                               @RequestParam(required = false) String weekdays) {
        ScheduleRule r = new ScheduleRule();
        r.setCounselorUsername(auth.getName());
        r.setFrequency(ScheduleFrequency.valueOf(frequency));
        r.setStartDate(LocalDate.parse(startDate));
        r.setEndDate(LocalDate.parse(endDate));
        r.setStartTime(LocalTime.parse(startTime));
        r.setEndTime(LocalTime.parse(endTime));
        if (weekdays != null && !weekdays.isEmpty()) {
            List<Integer> wd = Arrays.stream(weekdays.split(",")).map(String::trim).filter(s -> !s.isEmpty()).map(Integer::valueOf).collect(Collectors.toList());
            r.setWeekdays(wd);
        }
        return ResponseEntity.ok(ruleRepo.save(r));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(Authentication auth, @PathVariable Long id) {
        ScheduleRule r = ruleRepo.findById(id).orElse(null);
        if (r == null) return ResponseEntity.notFound().build();
        if (!r.getCounselorUsername().equals(auth.getName())) return ResponseEntity.status(403).build();
        ruleRepo.delete(r);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/exceptions")
    public ResponseEntity<ScheduleRuleException> addException(Authentication auth, @PathVariable Long id, @RequestParam String date) {
        ScheduleRule r = ruleRepo.findById(id).orElse(null);
        if (r == null) return ResponseEntity.notFound().build();
        if (!r.getCounselorUsername().equals(auth.getName())) return ResponseEntity.status(403).build();
        ScheduleRuleException ex = new ScheduleRuleException();
        ex.setRule(r);
        ex.setDate(LocalDate.parse(date));
        return ResponseEntity.ok(exRepo.save(ex));
    }

    @GetMapping("/{id}/exceptions")
    public ResponseEntity<List<ScheduleRuleException>> listExceptions(Authentication auth, @PathVariable Long id) {
        ScheduleRule r = ruleRepo.findById(id).orElse(null);
        if (r == null) return ResponseEntity.notFound().build();
        if (!r.getCounselorUsername().equals(auth.getName())) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(exRepo.findByRule(r));
    }

    @DeleteMapping("/exceptions/{exId}")
    public ResponseEntity<?> deleteException(Authentication auth, @PathVariable Long exId) {
        ScheduleRuleException ex = exRepo.findById(exId).orElse(null);
        if (ex == null) return ResponseEntity.notFound().build();
        ScheduleRule r = ex.getRule();
        if (r == null || !r.getCounselorUsername().equals(auth.getName())) return ResponseEntity.status(403).build();
        exRepo.delete(ex);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generate(Authentication auth, @RequestParam String from, @RequestParam String to) {
        int c = genService.generate(auth.getName(), LocalDate.parse(from), LocalDate.parse(to));
        return ResponseEntity.ok(String.valueOf(c));
    }
}
