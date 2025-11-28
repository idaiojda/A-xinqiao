package com.example.xinqiaobackend.repository;

import com.example.xinqiaobackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    @Query("select u from User u join u.roles r where r = :role and (:status is null or u.reviewStatus = :status) and (:q is null or u.username like concat('%', :q, '%'))")
    List<User> findByRoleAndStatus(@Param("role") String role, @Param("status") String status, @Param("q") String query);
}

