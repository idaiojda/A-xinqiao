package com.example.xinqiao.bean;

/**
 * Chat History Entity (Optimized)
 * Corresponds to database table: chat_history
 */
public class ChatHistory {
    // Message types
    public static final int TYPE_USER = 1;      // User message
    public static final int TYPE_AI = 2;        // AI response
    public static final int TYPE_SYSTEM = 3;    // System message
    
    // Old type mapping (for backward compatibility)
    public static final int OLD_TYPE_AI = 0;    // Old: 0 = AI
    public static final int OLD_TYPE_USER = 1;  // Old: 1 = User
    
    private long id;
    private long sessionId;
    private long userId;
    private String userName;        // For compatibility (from user_info)
    private String content;
    private int messageType;        // 1=user, 2=ai, 3=system
    private int type;               // For backward compatibility
    private int status;             // 1=normal, 0=deleted
    private int isRead;             // 0=unread, 1=read
    private long timestamp;         // For compatibility
    private long createdAt;         // Timestamp in milliseconds

    public ChatHistory() {
        this.status = 1;
        this.isRead = 0;
        this.messageType = TYPE_USER;
    }

    // Old constructor (backward compatibility)
    public ChatHistory(String userName, String content, int type, long timestamp) {
        this.userName = userName;
        this.content = content;
        this.type = type;
        this.timestamp = timestamp;
        this.sessionId = 0;
        this.status = 1;
        this.isRead = 0;
        // Convert old type to new message_type
        this.messageType = (type == OLD_TYPE_AI) ? TYPE_AI : TYPE_USER;
    }
    
    // Old constructor with sessionId (backward compatibility)
    public ChatHistory(String userName, String content, int type, long timestamp, int sessionId) {
        this.userName = userName;
        this.content = content;
        this.type = type;
        this.timestamp = timestamp;
        this.sessionId = sessionId;
        this.status = 1;
        this.isRead = 0;
        // Convert old type to new message_type
        this.messageType = (type == OLD_TYPE_AI) ? TYPE_AI : TYPE_USER;
    }
    
    // New constructor (recommended)
    public ChatHistory(long sessionId, long userId, String content, int messageType) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.content = content;
        this.messageType = messageType;
        this.timestamp = System.currentTimeMillis();
        this.createdAt = this.timestamp;
        this.status = 1;
        this.isRead = 0;
        // Set old type for compatibility
        this.type = (messageType == TYPE_AI) ? OLD_TYPE_AI : OLD_TYPE_USER;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getSessionId() {
        return sessionId;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getMessageType() {
        return messageType;
    }

    public void setMessageType(int messageType) {
        this.messageType = messageType;
        // Update old type for compatibility
        this.type = (messageType == TYPE_AI) ? OLD_TYPE_AI : OLD_TYPE_USER;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
        // Update new message_type
        this.messageType = (type == OLD_TYPE_AI) ? TYPE_AI : TYPE_USER;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getIsRead() {
        return isRead;
    }

    public void setIsRead(int isRead) {
        this.isRead = isRead;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        this.createdAt = timestamp;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
        this.timestamp = createdAt;
    }

    // Helper methods
    public boolean isUserMessage() {
        return messageType == TYPE_USER;
    }

    public boolean isAiMessage() {
        return messageType == TYPE_AI;
    }

    public boolean isSystemMessage() {
        return messageType == TYPE_SYSTEM;
    }

    public boolean isDeleted() {
        return status == 0;
    }

    public boolean isUnread() {
        return isRead == 0;
    }

    public void markAsRead() {
        this.isRead = 1;
    }

    public void markAsDeleted() {
        this.status = 0;
    }
}