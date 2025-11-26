package com.example.xinqiaobackend.controller;

import com.example.xinqiaobackend.entity.Appointment;
import com.example.xinqiaobackend.entity.AppointmentStatus;
import com.example.xinqiaobackend.repository.AppointmentRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/counselor/dashboard")
@PreAuthorize("hasRole('COUNSELOR')")
public class CounselorDashboardController {
    private final AppointmentRepository appointmentRepository;

    public CounselorDashboardController(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    @GetMapping("/overview")
    public Map<String, Object> overview(Authentication auth) {
        String c = auth.getName();
        List<Appointment> all = appointmentRepository.findByCounselorUsernameOrderByStartTimeAsc(c);
        int pending = 0, approved = 0, completed = 0, cancelled = 0, rejected = 0;
        for (Appointment a : all) {
            if (a.getStatus() == AppointmentStatus.PENDING) pending++;
            else if (a.getStatus() == AppointmentStatus.APPROVED) approved++;
            else if (a.getStatus() == AppointmentStatus.COMPLETED) completed++;
            else if (a.getStatus() == AppointmentStatus.CANCELLED) cancelled++;
            else if (a.getStatus() == AppointmentStatus.REJECTED) rejected++;
        }
        LocalDate now = LocalDate.now();
        LocalDateTime mStart = now.withDayOfMonth(1).atStartOfDay();
        LocalDateTime mEnd = now.plusMonths(1).withDayOfMonth(1).atStartOfDay();
        List<Appointment> monthAll = appointmentRepository.findByStartTimeBetween(mStart, mEnd);
        int monthMine = 0;
        for (Appointment a : monthAll) if (c.equals(a.getCounselorUsername())) monthMine++;
        Map<String, Object> res = new HashMap<String, Object>();
        res.put("pending", pending);
        res.put("approved", approved);
        res.put("completed", completed);
        res.put("cancelled", cancelled);
        res.put("rejected", rejected);
        res.put("monthTotal", monthMine);
        double approvalRate = (pending + approved + rejected) == 0 ? 0.0 : ((double) approved) / (pending + approved + rejected);
        res.put("approvalRate", approvalRate);
        return res;
    }
}