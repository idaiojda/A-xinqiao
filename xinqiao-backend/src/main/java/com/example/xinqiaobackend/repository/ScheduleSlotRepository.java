package com.example.xinqiaobackend.repository;

import com.example.xinqiaobackend.entity.ScheduleSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface ScheduleSlotRepository extends JpaRepository<ScheduleSlot, Long> {
    List<ScheduleSlot> findByCounselorUsernameOrderByStartTimeAsc(String counselorUsername);
    List<ScheduleSlot> findByCounselorUsernameAndStartTimeBetween(String counselorUsername, LocalDateTime from, LocalDateTime to);
    List<ScheduleSlot> findByCounselorUsernameAndStartTimeLessThanEqualAndEndTimeGreaterThanEqualAndAvailableTrue(String counselorUsername, LocalDateTime start, LocalDateTime end);
}
