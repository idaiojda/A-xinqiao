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
        helper = MySQLHelper.getInstance();
    }

    /**
     * 保存聊天记录
     */
    public boolean saveChatHistory(ChatHistory chatHistory) {
        boolean flag = false;
        Connection conn = null;
        try {
            conn = helper.getConnection();
            String sql = "INSERT INTO chat_history (userName, content, type, timestamp, sessionId) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, chatHistory.getUserName());
            pstmt.setString(2, chatHistory.getContent());
            pstmt.setInt(3, chatHistory.getType());
            pstmt.setLong(4, chatHistory.getTimestamp());
            pstmt.setInt(5, chatHistory.getSessionId());
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
     * 获取用户的聊天历史记录
     */
    public List<ChatHistory> getChatHistory(String userName) {
        return getChatHistory(userName, -1);
    }
    
    /**
     * 获取用户特定会话的聊天历史记录
     */
    public List<ChatHistory> getChatHistory(String userName, int sessionId) {
        List<ChatHistory> chatHistoryList = new ArrayList<>();
        String sql;
        if (sessionId > 0) {
            sql = "SELECT * FROM chat_history WHERE userName=? AND sessionId=? ORDER BY timestamp ASC";
        } else {
            sql = "SELECT * FROM chat_history WHERE userName=? ORDER BY timestamp ASC";
        }
        
        try (Connection conn = helper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userName);
            if (sessionId > 0) {
                pstmt.setInt(2, sessionId);
            }
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                ChatHistory chatHistory = new ChatHistory();
                chatHistory.setId(rs.getInt("_id"));
                chatHistory.setUserName(rs.getString("userName"));
                chatHistory.setContent(rs.getString("content"));
                chatHistory.setType(rs.getInt("type"));
                chatHistory.setTimestamp(rs.getLong("timestamp"));
                chatHistory.setSessionId(rs.getInt("sessionId"));
                chatHistoryList.add(chatHistory);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return chatHistoryList;
    }

    /**
     * 删除用户的聊天历史记录
     */
    public boolean deleteChatHistory(String userName) {
        boolean flag = false;
        String sql = "DELETE FROM chat_history WHERE userName=?";
        try (Connection conn = helper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userName);
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
    public void getChatHistoryAsync(String userName, ChatHistoryCallback callback) {
        new Thread(() -> {
            List<ChatHistory> result = getChatHistory(userName);
            android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            mainHandler.post(() -> callback.onResult(result));
        }).start();
    }
    
    /**
     * 异步获取用户特定会话的聊天历史记录
     */
    public void getChatHistoryAsync(String userName, int sessionId, ChatHistoryCallback callback) {
        new Thread(() -> {
            List<ChatHistory> result = getChatHistory(userName, sessionId);
            android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            mainHandler.post(() -> callback.onResult(result));
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