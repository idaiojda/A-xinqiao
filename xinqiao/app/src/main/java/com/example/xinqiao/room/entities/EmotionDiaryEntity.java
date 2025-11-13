package com.example.xinqiao.room.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "emotion_diaries")
public class EmotionDiaryEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String userName;
    public String date; // ISO-8601 yyyy-MM-dd
    public int mood;    // 1-10
    public String noteEncrypted; // AES-256 encrypted note
}

