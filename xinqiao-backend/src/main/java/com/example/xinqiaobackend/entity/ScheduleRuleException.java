package com.example.xinqiaobackend.entity;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "schedule_rule_exceptions")
public class ScheduleRuleException {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", nullable = false)
    private ScheduleRule rule;

    @Column(nullable = false)
    private LocalDate date;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ScheduleRule getRule() { return rule; }
    public void setRule(ScheduleRule rule) { this.rule = rule; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
}