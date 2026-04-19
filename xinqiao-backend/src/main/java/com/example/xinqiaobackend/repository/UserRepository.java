package com.example.xinqiaobackend.repository;

import com.example.xinqiaobackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    
    /**
     * 使用悲观锁查询用户（SELECT FOR UPDATE）
     * 用于防止并发操作时的数据竞争
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.username = :username")
    Optional<User> findByUsernameWithLock(@Param("username") String username);
    
    boolean existsByUsername(String username);
    @Query("select u from User u where (:role is null or locate(:role, u.rolesCsv) > 0) and (:status is null or u.reviewStatus = :status) and (:q is null or u.username like concat('%', :q, '%'))")
    List<User> findByRoleAndStatus(@Param("role") String role, @Param("status") String status, @Param("q") String query);
    
    @Query("select count(u) from User u where u.updatedAt between :start and :end")
    long countCreatedBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    @Query("select count(u) from User u where u.updatedAt between :start and :end")
    long countActiveBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
