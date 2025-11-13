package com.example.xinqiao.room.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.xinqiao.room.entities.ConsultationEntity;

import java.util.List;

@Dao
public interface ConsultationDao {
    @Query("SELECT * FROM consultations WHERE userName = :user ORDER BY date DESC")
    List<ConsultationEntity> getAll(String user);

    @Query("SELECT * FROM consultations WHERE userName = :user AND type = :type ORDER BY date DESC")
    List<ConsultationEntity> getByType(String user, String type);

    @Query("SELECT * FROM consultations WHERE userName = :user AND date BETWEEN :start AND :end ORDER BY date DESC")
    List<ConsultationEntity> getByDateRange(String user, String start, String end);

    @Query("SELECT * FROM consultations WHERE userName = :user AND sessionId = :sessionId LIMIT 1")
    ConsultationEntity getBySessionId(String user, String sessionId);

    @Insert
    long insert(ConsultationEntity entity);

    @Update
    int update(ConsultationEntity entity);

    @Query("UPDATE consultations SET messageCount = :count WHERE sessionId = :sessionId AND userName = :user")
    int updateMessageCount(String user, String sessionId, int count);
}
