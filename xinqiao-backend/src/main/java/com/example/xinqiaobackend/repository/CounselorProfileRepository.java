package com.example.xinqiaobackend.repository;

import com.example.xinqiaobackend.entity.CounselorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CounselorProfileRepository extends JpaRepository<CounselorProfile, Long> {
    Optional<CounselorProfile> findByUsername(String username);
    Optional<CounselorProfile> findTopByUsernameOrderByIdDesc(String username);
}