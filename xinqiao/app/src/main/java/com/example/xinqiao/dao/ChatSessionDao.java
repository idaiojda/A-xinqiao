package com.example.xinqiao.dao;

import android.content.Context;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.example.xinqiao.bean.ChatSession;
import com.example.xinqiao.mysql.MySQLHelper;

public class ChatSessionDao {
    private Context context;
    private MySQLHelper helper;

    public ChatSessionDao(Context context) {
        this.context = context;
        helper = MySQLHelper.getInstance();
    }

    /**
     * 创建聊天会话
     */
    public int createChatSession(ChatSession session) {
        int sessionId = -1;
        Connection conn = null;
        try {
            conn = helper.getConnection();
            String sql = "INSERT INTO chat_sessions (userName, title, createTime, updateTime) VALUES (?, ?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            pstmt.setString(1, session.getUserName());
            pstmt.setString(2, session.getTitle());
            pstmt.setLong(3, session.getCreateTime());
            pstmt.setLong(4, session.getUpdateTime());
            int result = pstmt.executeUpdate();
            if (result > 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    sessionId = rs.getInt(1);
                }
                rs.close();
            }
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                helper.releaseConnection(conn);
            }
        }
        return sessionId;
    }

    /**
     * 获取用户的所有聊天会话
     */
    public List<ChatSession> getChatSessions(String userName) {
        List<ChatSession> sessionList = new ArrayList<>();
        String sql = "SELECT * FROM chat_sessions WHERE userName=? ORDER BY updateTime DESC";
        try (Connection conn = helper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userName);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                ChatSession session = new ChatSession();
                session.setId(rs.getInt("_id"));
                session.setUserName(rs.getString("userName"));
                session.setTitle(rs.getString("title"));
                session.setCreateTime(rs.getLong("createTime"));
                session.setUpdateTime(rs.getLong("updateTime"));
                sessionList.add(session);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sessionList;
    }

    /**
     * 更新聊天会话
     */
    public boolean updateChatSession(ChatSession session) {
        boolean flag = false;
        String sql = "UPDATE chat_sessions SET title=?, updateTime=? WHERE _id=?";
        try (Connection conn = helper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, session.getTitle());
            pstmt.setLong(2, session.getUpdateTime());
            pstmt.setInt(3, session.getId());
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
     * 删除聊天会话
     */
    public boolean deleteChatSession(int sessionId) {
        boolean flag = false;
        String sql = "DELETE FROM chat_sessions WHERE _id=?";
        try (Connection conn = helper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, sessionId);
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
     * 异步创建聊天会话
     */
    public void createChatSessionAsync(ChatSession session, CreateSessionCallback callback) {
        new Thread(() -> {
            int sessionId = createChatSession(session);
            if (callback != null) {
                callback.onResult(sessionId);
            }
        }).start();
    }

    /**
     * 异步获取用户的所有聊天会话
     */
    public void getChatSessionsAsync(String userName, GetSessionsCallback callback) {
        new Thread(() -> {
            List<ChatSession> sessions = getChatSessions(userName);
            if (callback != null) {
                callback.onResult(sessions);
            }
        }).start();
    }

    /**
     * 异步更新聊天会话
     */
    public void updateChatSessionAsync(ChatSession session, UpdateSessionCallback callback) {
        new Thread(() -> {
            boolean success = updateChatSession(session);
            if (callback != null) {
                callback.onResult(success);
            }
        }).start();
    }

    /**
     * 异步删除聊天会话
     */
    public void deleteChatSessionAsync(int sessionId, DeleteSessionCallback callback) {
        new Thread(() -> {
            boolean success = deleteChatSession(sessionId);
            if (callback != null) {
                callback.onResult(success);
            }
        }).start();
    }
    
    /**
     * 获取最新的聊天会话
     */
    public ChatSession getLatestSession(String userName) {
        ChatSession session = null;
        String sql = "SELECT * FROM chat_sessions WHERE userName=? ORDER BY updateTime DESC LIMIT 1";
        try (Connection conn = helper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                session = new ChatSession();
                session.setId(rs.getInt("_id"));
                session.setUserName(rs.getString("userName"));
                session.setTitle(rs.getString("title"));
                session.setCreateTime(rs.getLong("createTime"));
                session.setUpdateTime(rs.getLong("updateTime"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return session;
    }
    
    /**
     * 异步获取最新的聊天会话
     */
    public void getLatestSessionAsync(String userName, GetSessionCallback callback) {
        new Thread(() -> {
            ChatSession session = getLatestSession(userName);
            if (callback != null) {
                callback.onResult(session);
            }
        }).start();
    }
    
    /**
     * 异步获取用户的所有聊天会话列表
     */
    public void getSessionListAsync(String userName, GetSessionListCallback callback) {
        new Thread(() -> {
            List<ChatSession> sessions = getChatSessions(userName);
            if (callback != null) {
                callback.onResult(sessions);
            }
        }).start();
    }
    
    /**
     * 异步更新会话标题
     */
    public void updateSessionTitleAsync(int sessionId, String title, UpdateSessionCallback callback) {
        new Thread(() -> {
            ChatSession session = new ChatSession();
            session.setId(sessionId);
            session.setTitle(title);
            session.setUpdateTime(System.currentTimeMillis());
            boolean success = updateChatSession(session);
            if (callback != null) {
                callback.onResult(success);
            }
        }).start();
    }
    
    /**
     * 删除用户的所有会话
     */
    public boolean deleteAllSessions(String userName) {
        boolean flag = false;
        String sql = "DELETE FROM chat_sessions WHERE userName=?";
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
     * 异步删除用户的所有会话
     */
    public void deleteAllSessionsAsync(String userName, DeleteCallback callback) {
        new Thread(() -> {
            boolean success = deleteAllSessions(userName);
            if (callback != null) {
                callback.onResult(success);
            }
        }).start();
    }

    public interface CreateSessionCallback {
        void onResult(int sessionId);
    }

    public interface GetSessionsCallback {
        void onResult(List<ChatSession> sessions);
    }

    public interface UpdateSessionCallback {
        void onResult(boolean success);
    }

    public interface DeleteSessionCallback {
        void onResult(boolean success);
    }
    
    public interface GetSessionCallback {
        void onResult(ChatSession session);
    }
    
    public interface GetSessionListCallback {
        void onResult(List<ChatSession> sessionList);
    }
    
    public interface DeleteCallback {
        void onResult(boolean success);
    }
}