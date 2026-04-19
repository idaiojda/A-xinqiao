package com.example.xinqiaobackend.repository;

import com.example.xinqiaobackend.entity.Appointment;
import com.example.xinqiaobackend.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByCounselorUsernameOrderByStartTimeAsc(String counselorUsername);
    List<Appointment> findByUserUsernameOrderByStartTimeAsc(String userUsername);
    List<Appointment> findByCounselorUsernameAndStatus(String counselorUsername, AppointmentStatus status);
    List<Appointment> findByStartTimeBetween(LocalDateTime from, LocalDateTime to);
    List<Appointment> findByCounselorUsernameAndStartTimeLessThanAndEndTimeGreaterThan(String counselorUsername, LocalDateTime end, LocalDateTime start);
    
    @Query("select coalesce(sum(a.price), 0.0) from Appointment a where a.status = 'COMPLETED'")
    Double sumCompletedRevenueTotal();
    
    @Query("select coalesce(sum(a.price), 0.0) from Appointment a where a.status = 'COMPLETED' and a.startTime between :start and :end")
    Double sumCompletedRevenueBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}