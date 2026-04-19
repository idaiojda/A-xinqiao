package com.example.xinqiaobackend.modules.application.repository;

import com.example.xinqiaobackend.modules.application.entity.CounselorApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface CounselorApplicationRepository extends JpaRepository<CounselorApplication, Long> {
    @Query("select a from CounselorApplication a where a.userId = :userId order by a.createdAt desc")
    List<CounselorApplication> findByUserId(@Param("userId") Long userId);

    @Query("select a from CounselorApplication a where a.userId = :userId and a.status = 'pending'")
    Optional<CounselorApplication> findPendingByUser(@Param("userId") Long userId);

    @Query("select a from CounselorApplication a where a.userId = :userId and a.status = 'approved'")
    Optional<CounselorApplication> findApprovedByUser(@Param("userId") Long userId);

    @Query("select a from CounselorApplication a where (:status is null or a.status = :status) and (:q is null or a.realName like concat('%', :q, '%') or concat(a.userId, '') like concat('%', :q, '%')) order by a.createdAt desc")
    List<CounselorApplication> adminList(@Param("status") String status, @Param("q") String query);
}
