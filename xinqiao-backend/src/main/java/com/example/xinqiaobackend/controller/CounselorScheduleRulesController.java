package com.example.xinqiaobackend.controller;

import com.example.xinqiaobackend.entity.ScheduleFrequency;
import com.example.xinqiaobackend.entity.ScheduleRule;
import com.example.xinqiaobackend.entity.ScheduleRuleConfig;
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
    private final ScheduleGenerationService genService;

    public CounselorScheduleRulesController(ScheduleRuleRepository ruleRepo,
                                            ScheduleGenerationService genService) {
        this.ruleRepo = ruleRepo;
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
                                               @RequestParam(required = false) String weekdays,
                                               @RequestParam(required = false) String exceptions) {
        ScheduleRule r = new ScheduleRule();
        r.setCounselorUsername(auth.getName());
        r.setFrequency(ScheduleFrequency.valueOf(frequency));
        r.setStartDate(LocalDate.parse(startDate));
        r.setEndDate(LocalDate.parse(endDate));
        r.setStartTime(LocalTime.parse(startTime));
        r.setEndTime(LocalTime.parse(endTime));
        ScheduleRuleConfig cfg = new ScheduleRuleConfig();
        if (weekdays != null && !weekdays.isEmpty()) {
            List<Integer> wd = Arrays.stream(weekdays.split(",")).map(String::trim).filter(s -> !s.isEmpty()).map(Integer::valueOf).collect(Collectors.toList());
            cfg.setWeekdays(wd);
        }
        if (exceptions != null && !exceptions.isEmpty()) {
            List<String> ex = Arrays.stream(exceptions.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
            cfg.setExceptions(ex);
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            r.setConfig(om.writeValueAsString(cfg));
        } catch (Exception ignored) {}
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
    public ResponseEntity<?> addException(Authentication auth, @PathVariable Long id, @RequestParam String date) {
        ScheduleRule r = ruleRepo.findById(id).orElse(null);
        if (r == null) return ResponseEntity.notFound().build();
        if (!r.getCounselorUsername().equals(auth.getName())) return ResponseEntity.status(403).build();
        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
        try {
            ScheduleRuleConfig cfg = r.getConfig() != null && !r.getConfig().isEmpty() ? om.readValue(r.getConfig(), ScheduleRuleConfig.class) : new ScheduleRuleConfig();
            java.util.List<String> ex = cfg.getExceptions();
            if (ex == null) ex = new java.util.ArrayList<>();
            ex.add(date);
            cfg.setExceptions(ex);
            r.setConfig(om.writeValueAsString(cfg));
            ruleRepo.save(r);
        } catch (Exception ignored) {}
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/exceptions")
    public ResponseEntity<List<String>> listExceptions(Authentication auth, @PathVariable Long id) {
        ScheduleRule r = ruleRepo.findById(id).orElse(null);
        if (r == null) return ResponseEntity.notFound().build();
        if (!r.getCounselorUsername().equals(auth.getName())) return ResponseEntity.status(403).build();
        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
        try {
            ScheduleRuleConfig cfg = r.getConfig() != null && !r.getConfig().isEmpty() ? om.readValue(r.getConfig(), ScheduleRuleConfig.class) : new ScheduleRuleConfig();
            return ResponseEntity.ok(cfg.getExceptions());
        } catch (Exception ignored) {
            return ResponseEntity.ok(java.util.Collections.emptyList());
        }
    }

    @DeleteMapping("/exceptions/{id}")
    public ResponseEntity<?> deleteException(Authentication auth, @PathVariable Long id) {
        ScheduleRule r = ruleRepo.findById(id).orElse(null);
        if (r == null) return ResponseEntity.notFound().build();
        if (!r.getCounselorUsername().equals(auth.getName())) return ResponseEntity.status(403).build();
        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
        try {
            ScheduleRuleConfig cfg = r.getConfig() != null && !r.getConfig().isEmpty() ? om.readValue(r.getConfig(), ScheduleRuleConfig.class) : new ScheduleRuleConfig();
            java.util.List<String> ex = cfg.getExceptions();
            if (ex != null && !ex.isEmpty()) {
                ex.remove(ex.size() - 1);
            }
            cfg.setExceptions(ex);
            r.setConfig(om.writeValueAsString(cfg));
            ruleRepo.save(r);
        } catch (Exception ignored) {}
        return ResponseEntity.ok().build();
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generate(Authentication auth, @RequestParam String from, @RequestParam String to) {
        int c = genService.generate(auth.getName(), LocalDate.parse(from), LocalDate.parse(to));
        return ResponseEntity.ok(String.valueOf(c));
    }
}
