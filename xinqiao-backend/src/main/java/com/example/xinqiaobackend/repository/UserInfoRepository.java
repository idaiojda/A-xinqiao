package com.example.xinqiaobackend.repository;

import com.example.xinqiaobackend.entity.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserInfoRepository extends JpaRepository<UserInfo, Long> {
    @Query("select u from UserInfo u where (:q is null or u.username like concat('%', :q, '%'))")
    List<UserInfo> search(@Param("q") String q);
}
