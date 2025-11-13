package com.example.xinqiao.bean;

public class ConsultationItem {
    public long id;
    public String sessionId;
    public String type; // ai or pro
    public String title;
    public String date; // yyyy-MM-dd
    public int messageCount;
    public String status; // optional
    public String summary; // decrypted

    public ConsultationItem() {}
}
