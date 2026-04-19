package com.example.xinqiao.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.example.xinqiao.bean.ChatHistory;
import com.example.xinqiao.mysql.MySQLHelper;

import java.util.ArrayList;
import java.util.List;

public class ChatHistoryDao {
    private Context context;
    private MySQLHelper helper;

    public ChatHistoryDao(Context context) {
        this.context = context;
        try {
            helper = MySQLHelper.getInstance();
        } catch (IllegalStateException e) {
            helper = null;
            try {
                MySQLHelper.getInstance(context, new MySQLHelper.InitCallback() {
                    @Override public void onSuccess() { try { ChatHistoryDao.this.helper = MySQLHelper.getInstance(); } catch (IllegalStateException ignored) {} }
                    @Override public void onError(SQLException ex) {}
                });
            } catch (Throwable ignored) {}
        }
    }

    /**
     * 保存聊天记录
     */
    public boolean saveChatHistory(ChatHistory chatHistory) {
        boolean flag = false;
        if (!ensureHelper()) return false;
        Connection conn = null;
        try {
            conn = helper.getConnection();
            
            // 如果 userId 为 0 但有 userName,尝试通过 userName 获取 userId
            long userId = chatHistory.getUserId();
            if (userId <= 0 && chatHistory.getUserName() != null && !chatHistory.getUserName().isEmpty()) {
                userId = getUserIdByUserName(chatHistory.getUserName());
                if (userId > 0) {
                    chatHistory.setUserId(userId);
                }
            }
            
            // 如果还是没有有效的 userId,返回失败
            if (userId <= 0) {
                System.err.println("无法保存聊天记录: userId 无效 (userId=" + userId + ", userName=" + chatHistory.getUserName() + ")");
                return false;
            }
            
            String sql = "INSERT INTO chat_history (user_id, content, message_type, session_id) VALUES (?, ?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, userId);
            pstmt.setString(2, chatHistory.getContent());
            pstmt.setInt(3, chatHistory.getMessageType());
            pstmt.setLong(4, chatHistory.getSessionId());
            int result = pstmt.executeUpdate();
            if (result > 0) {
                flag = true;
            }
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                helper.releaseConnection(conn);
            }
        }
        return flag;
    }

    /**
     * 获取用户的聊天历史记录（按用户ID）
     */
    public List<ChatHistory> getChatHistory(long userId) {
        return getChatHistoryByUserId(userId, -1);
    }
    
    /**
     * 获取用户特定会话的聊天历史记录（按用户ID）
     */
    public List<ChatHistory> getChatHistoryByUserId(long userId, long sessionId) {
        List<ChatHistory> chatHistoryList = new ArrayList<>();
        if (!ensureHelper()) return chatHistoryList;
        String sql;
        if (sessionId > 0) {
            sql = "SELECT * FROM chat_history WHERE user_id=? AND session_id=? AND status=1 ORDER BY created_at ASC";
        } else {
            sql = "SELECT * FROM chat_history WHERE user_id=? AND status=1 ORDER BY created_at ASC";
        }
        
        try (Connection conn = helper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            if (sessionId > 0) {
                pstmt.setLong(2, sessionId);
            }
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                ChatHistory chatHistory = new ChatHistory();
                chatHistory.setId(rs.getLong("id"));
                chatHistory.setUserId(rs.getLong("user_id"));
                chatHistory.setContent(rs.getString("content"));
                chatHistory.setMessageType(rs.getInt("message_type"));
                chatHistory.setSessionId(rs.getLong("session_id"));
                chatHistory.setStatus(rs.getInt("status"));
                chatHistory.setIsRead(rs.getInt("is_read"));
                chatHistoryList.add(chatHistory);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return chatHistoryList;
    }
    
    /**
     * 获取会话的聊天历史记录
     */
    public List<ChatHistory> getChatHistoryBySessionId(long sessionId) {
        List<ChatHistory> chatHistoryList = new ArrayList<>();
        if (!ensureHelper()) return chatHistoryList;
        String sql = "SELECT * FROM chat_history WHERE session_id=? AND status=1 ORDER BY created_at ASC";
        
        try (Connection conn = helper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, sessionId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                ChatHistory chatHistory = new ChatHistory();
                chatHistory.setId(rs.getLong("id"));
                chatHistory.setUserId(rs.getLong("user_id"));
                chatHistory.setContent(rs.getString("content"));
                chatHistory.setMessageType(rs.getInt("message_type"));
                chatHistory.setSessionId(rs.getLong("session_id"));
                chatHistory.setStatus(rs.getInt("status"));
                chatHistory.setIsRead(rs.getInt("is_read"));
                chatHistoryList.add(chatHistory);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return chatHistoryList;
    }

    /**
     * 删除用户的聊天历史记录（软删除）
     */
    public boolean deleteChatHistory(long userId) {
        boolean flag = false;
        if (!ensureHelper()) return false;
        String sql = "UPDATE chat_history SET status=0 WHERE user_id=?";
        try (Connection conn = helper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            int result = pstmt.executeUpdate();
            if (result > 0) {
                flag = true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return flag;
    }

    /**
     * 删除单条消息（软删除）
     */
    public boolean deleteMessageById(long id) {
        boolean flag = false;
        if (!ensureHelper()) return false;
        String sql = "UPDATE chat_history SET status=0 WHERE id=?";
        try (Connection conn = helper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            int result = pstmt.executeUpdate();
            if (result > 0) {
                flag = true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return flag;
    }
    
    /**
     * 物理删除单条消息（谨慎使用）
     */
    public boolean deleteMessagePermanently(long id) {
        boolean flag = false;
        if (!ensureHelper()) return false;
        String sql = "DELETE FROM chat_history WHERE id=?";
        try (Connection conn = helper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            int result = pstmt.executeUpdate();
            if (result > 0) {
                flag = true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return flag;
    }
    
    /**
     * 标记消息为已读
     */
    public boolean markAsRead(long messageId) {
        boolean flag = false;
        if (!ensureHelper()) return false;
        String sql = "UPDATE chat_history SET is_read=1 WHERE id=?";
        try (Connection conn = helper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, messageId);
            int result = pstmt.executeUpdate();
            if (result > 0) {
                flag = true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return flag;
    }
    
    /**
     * 标记会话所有消息为已读
     */
    public boolean markSessionAsRead(long sessionId) {
        boolean flag = false;
        if (!ensureHelper()) return false;
        String sql = "UPDATE chat_history SET is_read=1 WHERE session_id=? AND is_read=0";
        try (Connection conn = helper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, sessionId);
            int result = pstmt.executeUpdate();
            if (result > 0) {
                flag = true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return flag;
    }
    
    /**
     * 获取未读消息数
     */
    public int getUnreadCount(long sessionId) {
        int count = 0;
        if (!ensureHelper()) return count;
        String sql = "SELECT COUNT(*) FROM chat_history WHERE session_id=? AND is_read=0 AND status=1";
        try (Connection conn = helper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, sessionId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                count = rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return count;
    }

    private boolean ensureHelper() {
        if (helper == null) {
            try { helper = MySQLHelper.getInstance(); } catch (IllegalStateException e) { return false; }
        }
        return true;
    }

    /**
     * 异步保存聊天记录
     */
    public void saveChatHistoryAsync(ChatHistory chatHistory, SimpleResultCallback callback) {
        new Thread(() -> {
            boolean result = saveChatHistory(chatHistory);
            android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            mainHandler.post(() -> callback.onResult(result));
        }).start();
    }

    /**
     * 异步获取用户的聊天历史记录
     */
    public void getChatHistoryAsync(long userId, ChatHistoryCallback callback) {
        new Thread(() -> {
            List<ChatHistory> result = getChatHistory(userId);
            android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            mainHandler.post(() -> callback.onResult(result));
        }).start();
    }
    
    /**
     * 异步获取用户特定会话的聊天历史记录
     */
    public void getChatHistoryAsync(long userId, long sessionId, ChatHistoryCallback callback) {
        new Thread(() -> {
            List<ChatHistory> result = getChatHistoryByUserId(userId, sessionId);
            android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            mainHandler.post(() -> callback.onResult(result));
        }).start();
    }
    
    /**
     * 异步获取会话的聊天历史记录
     */
    public void getChatHistoryBySessionIdAsync(long sessionId, ChatHistoryCallback callback) {
        new Thread(() -> {
            List<ChatHistory> result = getChatHistoryBySessionId(sessionId);
            android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            mainHandler.post(() -> callback.onResult(result));
        }).start();
    }
    
    /**
     * 异步获取用户的聊天历史记录（通过用户名 - 兼容旧代码）
     */
    public void getChatHistoryAsync(String userName, int sessionId, ChatHistoryCallback callback) {
        new Thread(() -> {
            // Convert userName to userId
            long userId = getUserIdByUserName(userName);
            if (userId <= 0) {
                android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                mainHandler.post(() -> callback.onResult(new ArrayList<>()));
                return;
            }
            List<ChatHistory> result = getChatHistoryByUserId(userId, sessionId);
            android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            mainHandler.post(() -> callback.onResult(result));
        }).start();
    }
    
    /**
     * 通过用户名获取用户ID
     */
    private long getUserIdByUserName(String userName) {
        if (!ensureHelper() || userName == null || userName.isEmpty()) return 0;
        String sql = "SELECT user_id FROM user_info WHERE username=?";
        try (Connection conn = helper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getLong("user_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 异步删除单条消息（软删除）
     */
    public void deleteMessageByIdAsync(long id, SimpleResultCallback callback) {
        new Thread(() -> {
            boolean result = deleteMessageById(id);
            android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            mainHandler.post(() -> {
                if (callback != null) callback.onResult(result);
            });
        }).start();
    }
    
    /**
     * 异步标记消息为已读
     */
    public void markAsReadAsync(long messageId, SimpleResultCallback callback) {
        new Thread(() -> {
            boolean result = markAsRead(messageId);
            android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            mainHandler.post(() -> {
                if (callback != null) callback.onResult(result);
            });
        }).start();
    }
    
    /**
     * 异步标记会话为已读
     */
    public void markSessionAsReadAsync(long sessionId, SimpleResultCallback callback) {
        new Thread(() -> {
            boolean result = markSessionAsRead(sessionId);
            android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            mainHandler.post(() -> {
                if (callback != null) callback.onResult(result);
            });
        }).start();
    }

    /**
     * 简单结果回调接口
     */
    public interface SimpleResultCallback {
        void onResult(boolean success);
    }

    /**
     * 聊天历史回调接口
     */
    public interface ChatHistoryCallback {
        void onResult(List<ChatHistory> history);
    }
}
