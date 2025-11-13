package com.example.xinqiao.room.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.xinqiao.room.entities.AuthorizationEntity;

import java.util.List;

@Dao
public interface AuthorizationDao {
    @Query("SELECT * FROM authorizations WHERE userName = :user ORDER BY startTimestamp DESC")
    List<AuthorizationEntity> getAll(String user);

    @Query("SELECT * FROM authorizations WHERE userName = :user AND endTimestamp >= :now ORDER BY endTimestamp ASC")
    List<AuthorizationEntity> getActive(String user, long now);

    @Insert
    long insert(AuthorizationEntity entity);

    @Update
    int update(AuthorizationEntity entity);
}

