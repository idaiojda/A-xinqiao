package com.example.xinqiao.bean;

public class ChatHistory {
    private int id;
    private String userName;
    private String content;
    private int type;
    private long timestamp;
    private int sessionId;

    public ChatHistory() {
    }

    public ChatHistory(String userName, String content, int type, long timestamp) {
        this.userName = userName;
        this.content = content;
        this.type = type;
        this.timestamp = timestamp;
        this.sessionId = 0;
    }
    
    public ChatHistory(String userName, String content, int type, long timestamp, int sessionId) {
        this.userName = userName;
        this.content = content;
        this.type = type;
        this.timestamp = timestamp;
        this.sessionId = sessionId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
    
    public int getSessionId() {
        return sessionId;
    }

    public void setSessionId(int sessionId) {
        this.sessionId = sessionId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}