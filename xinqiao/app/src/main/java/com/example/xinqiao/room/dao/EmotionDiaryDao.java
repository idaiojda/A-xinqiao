package com.example.xinqiao.room.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.xinqiao.room.entities.EmotionDiaryEntity;

import java.util.List;

@Dao
public interface EmotionDiaryDao {
    @Query("SELECT * FROM emotion_diaries WHERE userName = :user ORDER BY date DESC")
    List<EmotionDiaryEntity> getAll(String user);

    @Query("SELECT * FROM emotion_diaries WHERE userName = :user AND date BETWEEN :start AND :end ORDER BY date ASC")
    List<EmotionDiaryEntity> getByDateRange(String user, String start, String end);

    @Query("SELECT * FROM emotion_diaries ORDER BY date DESC")
    List<EmotionDiaryEntity> getAllAnyUser();

    @Query("SELECT * FROM emotion_diaries WHERE date BETWEEN :start AND :end ORDER BY date ASC")
    List<EmotionDiaryEntity> getByDateRangeAnyUser(String start, String end);

    @Insert
    long insert(EmotionDiaryEntity entity);

    @Update
    int update(EmotionDiaryEntity entity);

    @Delete
    int delete(EmotionDiaryEntity entity);

    @Query("DELETE FROM emotion_diaries WHERE id = :id")
    int deleteById(long id);
}
