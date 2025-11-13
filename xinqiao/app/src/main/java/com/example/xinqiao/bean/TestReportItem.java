package com.example.xinqiao.bean;

public class TestReportItem {
    public long id;
    public String reportId;
    public String type; // e.g., PHQ9/GAD7
    public float score;
    public String riskLevel;
    public String date; // yyyy-MM-dd
    public String details; // decrypted preview

    public TestReportItem() {}
}
