package com.example.xinqiao.room.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "consultations")
public class ConsultationEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String userName;
    public String sessionId;
    public String type; // "ai" or "pro"
    public String title;
    public String date; // ISO-8601 yyyy-MM-dd
    public int messageCount;
    public String summaryEncrypted; // AES-256 encrypted summary or key points
    public String status; // e.g., "已完成" / "已确认"
    public String interventionMeasures; // 干预措施
}

