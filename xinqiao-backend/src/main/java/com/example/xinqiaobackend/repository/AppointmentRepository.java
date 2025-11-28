package com.example.xinqiaobackend.repository;

import com.example.xinqiaobackend.entity.Appointment;
import com.example.xinqiaobackend.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByCounselorUsernameOrderByStartTimeAsc(String counselorUsername);
    List<Appointment> findByUserUsernameOrderByStartTimeAsc(String userUsername);
    List<Appointment> findByCounselorUsernameAndStatus(String counselorUsername, AppointmentStatus status);
    List<Appointment> findByStartTimeBetween(LocalDateTime from, LocalDateTime to);
    List<Appointment> findByCounselorUsernameAndStartTimeLessThanAndEndTimeGreaterThan(String counselorUsername, LocalDateTime end, LocalDateTime start);
}