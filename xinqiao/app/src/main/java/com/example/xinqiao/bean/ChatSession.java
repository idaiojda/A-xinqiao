package com.example.xinqiao.bean;

import java.util.Date;

public class ChatSession {
    private int id;
    private String userName;
    private String title;
    private long createTime;
    private long updateTime;

    public ChatSession() {
    }

    public ChatSession(String userName, String title, long createTime, long updateTime) {
        this.userName = userName;
        this.title = title;
        this.createTime = createTime;
        this.updateTime = updateTime;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    public String getFormattedTime() {
        Date date = new Date(updateTime);
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
        return sdf.format(date);
    }
}