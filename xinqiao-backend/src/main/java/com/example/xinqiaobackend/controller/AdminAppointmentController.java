package com.example.xinqiaobackend.controller;

import com.example.xinqiaobackend.entity.Appointment;
import com.example.xinqiaobackend.entity.AppointmentStatus;
import com.example.xinqiaobackend.repository.AppointmentRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/appointments")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAppointmentController {
    private final AppointmentRepository repo;

    public AdminAppointmentController(AppointmentRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Appointment> list(@RequestParam(required = false) String status) {
        if (status == null || status.isEmpty()) {
            return repo.findAll();
        }
        AppointmentStatus st;
        try {
            st = AppointmentStatus.valueOf(status);
        } catch (Exception e) {
            return repo.findAll();
        }
        return repo.findAll().stream().filter(a -> a.getStatus() == st).collect(Collectors.toList());
    }
}