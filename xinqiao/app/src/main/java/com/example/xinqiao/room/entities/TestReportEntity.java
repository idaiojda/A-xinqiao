package com.example.xinqiao.room.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "test_reports")
public class TestReportEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String userName;
    public String reportId; // external id or uuid
    public String type;     // e.g., "SDS", "SAS", "other"
    public int score;
    public String riskLevel; // "低风险" / "中风险" / "高风险"
    public String date;      // ISO-8601 yyyy-MM-dd
    public String detailsEncrypted; // AES-256 encrypted details
}

