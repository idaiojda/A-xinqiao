package com.example.xinqiao.dao;

import android.content.Context;
import com.example.xinqiao.mysql.MySQLHelper;
import com.example.xinqiao.bean.ChatHistory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MySQLChatHistoryDao {
    private Context context;
    private MySQLHelper mySQLHelper;

    public MySQLChatHistoryDao(Context context) {
        this.context = context;
        this.mySQLHelper = MySQLHelper.getInstance();
    }

    /**
     * 保存聊天记录
     */
    public boolean saveChatHistory(ChatHistory chatHistory) {
        boolean flag = false;
        try {
            Connection conn = mySQLHelper.getConnection();
            String sql = "INSERT INTO chat_history (userName, content, type, timestamp) VALUES (?, ?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, chatHistory.getUserName());
            pstmt.setString(2, chatHistory.getContent());
            pstmt.setInt(3, chatHistory.getType());
            pstmt.setLong(4, chatHistory.getTimestamp());
            
            int result = pstmt.executeUpdate();
            if (result > 0) {
                flag = true;
            }
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return flag;
    }

    /**
     * 获取用户的聊天历史记录
     */
    public List<ChatHistory> getChatHistory(String userName) {
        List<ChatHistory> chatHistoryList = new ArrayList<>();
        try {
            Connection conn = mySQLHelper.getConnection();
            String sql = "SELECT * FROM chat_history WHERE userName=? ORDER BY timestamp ASC";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, userName);
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                ChatHistory chatHistory = new ChatHistory();
                chatHistory.setId(rs.getInt("_id"));
                chatHistory.setUserName(rs.getString("userName"));
                chatHistory.setContent(rs.getString("content"));
                chatHistory.setType(rs.getInt("type"));
                chatHistory.setTimestamp(rs.getLong("timestamp"));
                chatHistoryList.add(chatHistory);
            }
            rs.close();
            pstmt.close();
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
        try {
            Connection conn = mySQLHelper.getConnection();
            String sql = "DELETE FROM chat_history WHERE userName=?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, userName);
            
            int result = pstmt.executeUpdate();
            if (result > 0) {
                flag = true;
            }
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return flag;
    }
}