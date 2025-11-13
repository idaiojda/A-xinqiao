package com.example.xinqiao.bean;

public class AIConsultationEntry {
    public long id;           // 唯一ID（时间戳）
    public String userName;   // 用户名
    public String date;       // 最近更新时间 yyyy-MM-dd HH:mm
    public int sessionId;     // 会话ID
    public String title;      // 会话标题/摘要
    public int messageCount;  // 消息条数

    public AIConsultationEntry() {}

    public AIConsultationEntry(long id, String userName, String date, int sessionId, String title, int messageCount) {
        this.id = id;
        this.userName = userName;
        this.date = date;
        this.sessionId = sessionId;
        this.title = title;
        this.messageCount = messageCount;
    }
}
