package com.example.xinqiao.bean;

import java.util.Date;

/**
 * 聊天会话实体类（优化版）
 * 对应数据库表：chat_sessions
 */
public class ChatSession {
    private long id;              // 改为 long 类型
    private long userId;          // 新增：用户ID（替代 userName）
    private String userName;      // 保留：用于兼容性（从 user_info 查询）
    private String title;
    private String lastMessage;   // 新增：最后一条消息
    private int unreadCount;      // 新增：未读消息数
    private int status;           // 新增：状态（1-正常，0-已删除）
    private long createTime;      // 保留：用于兼容性
    private long updateTime;      // 保留：用于兼容性

    public ChatSession() {
        this.status = 1;
        this.unreadCount = 0;
    }

    public ChatSession(String userName, String title, long createTime, long updateTime) {
        this.userName = userName;
        this.title = title;
        this.createTime = createTime;
        this.updateTime = updateTime;
        this.status = 1;
        this.unreadCount = 0;
    }

    public ChatSession(long userId, String title, long createTime, long updateTime) {
        this.userId = userId;
        this.title = title;
        this.createTime = createTime;
        this.updateTime = updateTime;
        this.status = 1;
        this.unreadCount = 0;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
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

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
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

    /**
     * 获取最后消息的预览文本（限制长度）
     */
    public String getLastMessagePreview() {
        if (lastMessage == null || lastMessage.isEmpty()) {
            return "暂无消息";
        }
        if (lastMessage.length() > 50) {
            return lastMessage.substring(0, 50) + "...";
        }
        return lastMessage;
    }

    /**
     * 是否有未读消息
     */
    public boolean hasUnread() {
        return unreadCount > 0;
    }

    /**
     * 是否已删除
     */
    public boolean isDeleted() {
        return status == 0;
    }
}