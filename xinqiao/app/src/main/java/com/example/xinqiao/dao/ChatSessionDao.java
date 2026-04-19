package com.example.xinqiao.dao;

import android.content.Context;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.example.xinqiao.bean.ChatSession;
import com.example.xinqiao.mysql.MySQLHelper;

/**
 * 聊天会话数据访问对象（优化版）
 * 适配新的 chat_sessions 表结构
 */
public class ChatSessionDao {
    private Context context;
    private MySQLHelper helper;

    public ChatSessionDao(Context context) {
        this.context = context;
        helper = MySQLHelper.getInstance();
    }

    /**
     * 创建聊天会话（新版：使用 user_id）
     */
    public long createChatSession(ChatSession session) {
        long sessionId = -1;
        Connection conn = null;
        try {
            conn = helper.getConnection();
            String sql = "INSERT INTO chat_sessions (user_id, title, created_at, updated_at, status) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            pstmt.setLong(1, session.getUserId());
            pstmt.setString(2, session.getTitle());
            pstmt.setTimestamp(3, new Timestamp(session.getCreateTime()));
            pstmt.setTimestamp(4, new Timestamp(session.getUpdateTime()));
            pstmt.setInt(5, session.getStatus());
            int result = pstmt.executeUpdate();
            if (result > 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    sessionId = rs.getLong(1);
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
     * 创建聊天会话（兼容旧版：使用 userName）
     * 自动查询 user_id
     */
    public long createChatSessionByUsername(String userName, String title) {
        long sessionId = -1;
        Connection conn = null;
        try {
            conn = helper.getConnection();
            // 先查询 user_id
            String queryUserId = "SELECT user_id FROM user_info WHERE username = ?";
            PreparedStatement queryStmt = conn.prepareStatement(queryUserId);
            queryStmt.setString(1, userName);
            ResultSet rs = queryStmt.executeQuery();
            
            if (rs.next()) {
                long userId = rs.getLong("user_id");
                rs.close();
                queryStmt.close();
                
                // 创建会话
                String sql = "INSERT INTO chat_sessions (user_id, title, status) VALUES (?, ?, 1)";
                PreparedStatement pstmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
                pstmt.setLong(1, userId);
                pstmt.setString(2, title);
                int result = pstmt.executeUpdate();
                if (result > 0) {
                    ResultSet keys = pstmt.getGeneratedKeys();
                    if (keys.next()) {
                        sessionId = keys.getLong(1);
                    }
                    keys.close();
                }
                pstmt.close();
            }
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
     * 获取用户的所有聊天会话（新版：使用 user_id）
     */
    public List<ChatSession> getChatSessionsByUserId(long userId) {
        List<ChatSession> sessionList = new ArrayList<>();
        String sql = "SELECT cs.*, ui.username FROM chat_sessions cs " +
                     "LEFT JOIN user_info ui ON cs.user_id = ui.user_id " +
                     "WHERE cs.user_id = ? AND cs.status = 1 " +
                     "ORDER BY cs.updated_at DESC";
        try (Connection conn = helper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                ChatSession session = new ChatSession();
                session.setId(rs.getLong("id"));
                session.setUserId(rs.getLong("user_id"));
                session.setUserName(rs.getString("username"));
                session.setTitle(rs.getString("title"));
                session.setLastMessage(rs.getString("last_message"));
                session.setUnreadCount(rs.getInt("unread_count"));
                session.setStatus(rs.getInt("status"));
                Timestamp createdAt = rs.getTimestamp("created_at");
                Timestamp updatedAt = rs.getTimestamp("updated_at");
                session.setCreateTime(createdAt != null ? createdAt.getTime() : 0);
                session.setUpdateTime(updatedAt != null ? updatedAt.getTime() : 0);
                sessionList.add(session);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sessionList;
    }

    /**
     * 获取用户的所有聊天会话（兼容旧版：使用 userName）
     */
    public List<ChatSession> getChatSessions(String userName) {
        List<ChatSession> sessionList = new ArrayList<>();
        String sql = "SELECT cs.*, ui.username FROM chat_sessions cs " +
                     "INNER JOIN user_info ui ON cs.user_id = ui.user_id " +
                     "WHERE ui.username = ? AND cs.status = 1 " +
                     "ORDER BY cs.updated_at DESC";
        try (Connection conn = helper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userName);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                ChatSession session = new ChatSession();
                session.setId(rs.getLong("id"));
                session.setUserId(rs.getLong("user_id"));
                session.setUserName(rs.getString("username"));
                session.setTitle(rs.getString("title"));
                session.setLastMessage(rs.getString("last_message"));
                session.setUnreadCount(rs.getInt("unread_count"));
                session.setStatus(rs.getInt("status"));
                Timestamp createdAt = rs.getTimestamp("created_at");
                Timestamp updatedAt = rs.getTimestamp("updated_at");
                session.setCreateTime(createdAt != null ? createdAt.getTime() : 0);
                session.setUpdateTime(updatedAt != null ? updatedAt.getTime() : 0);
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
        String sql = "UPDATE chat_sessions SET title=?, last_message=?, unread_count=?, updated_at=? WHERE id=?";
        try (Connection conn = helper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, session.getTitle());
            pstmt.setString(2, session.getLastMessage());
            pstmt.setInt(3, session.getUnreadCount());
            pstmt.setTimestamp(4, new Timestamp(session.getUpdateTime()));
            pstmt.setLong(5, session.getId());
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
     * 更新会话标题
     */
    public boolean updateSessionTitle(long sessionId, String title) {
        boolean flag = false;
        String sql = "UPDATE chat_sessions SET title=?, updated_at=NOW() WHERE id=?";
        try (Connection conn = helper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, title);
            pstmt.setLong(2, sessionId);
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
     * 更新最后一条消息
     */
    public boolean updateLastMessage(long sessionId, String lastMessage) {
        boolean flag = false;
        String sql = "UPDATE chat_sessions SET last_message=?, updated_at=NOW() WHERE id=?";
        try (Connection conn = helper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, lastMessage);
            pstmt.setLong(2, sessionId);
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
     * 更新未读消息数
     */
    public boolean updateUnreadCount(long sessionId, int count) {
        boolean flag = false;
        String sql = "UPDATE chat_sessions SET unread_count=?, updated_at=NOW() WHERE id=?";
        try (Connection conn = helper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, count);
            pstmt.setLong(2, sessionId);
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
     * 清空未读消息数
     */
    public boolean clearUnreadCount(long sessionId) {
        return updateUnreadCount(sessionId, 0);
    }

    /**
     * 删除聊天会话（软删除）
     */
    public boolean deleteChatSession(long sessionId) {
        boolean flag = false;
        String sql = "UPDATE chat_sessions SET status=0, updated_at=NOW() WHERE id=?";
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
     * 物理删除聊天会话（谨慎使用）
     */
    public boolean deleteChatSessionPermanently(long sessionId) {
        boolean flag = false;
        String sql = "DELETE FROM chat_sessions WHERE id=?";
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
     * 异步创建聊天会话
     */
    public void createChatSessionAsync(ChatSession session, CreateSessionCallback callback) {
        new Thread(() -> {
            long sessionId = createChatSession(session);
            if (callback != null) {
                callback.onResult(sessionId);
            }
        }).start();
    }

    /**
     * 异步创建聊天会话（使用 userName）
     */
    public void createChatSessionByUsernameAsync(String userName, String title, CreateSessionCallback callback) {
        new Thread(() -> {
            long sessionId = createChatSessionByUsername(userName, title);
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
    public void deleteChatSessionAsync(long sessionId, DeleteSessionCallback callback) {
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
        String sql = "SELECT cs.*, ui.username FROM chat_sessions cs " +
                     "INNER JOIN user_info ui ON cs.user_id = ui.user_id " +
                     "WHERE ui.username = ? AND cs.status = 1 " +
                     "ORDER BY cs.updated_at DESC LIMIT 1";
        try (Connection conn = helper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                session = new ChatSession();
                session.setId(rs.getLong("id"));
                session.setUserId(rs.getLong("user_id"));
                session.setUserName(rs.getString("username"));
                session.setTitle(rs.getString("title"));
                session.setLastMessage(rs.getString("last_message"));
                session.setUnreadCount(rs.getInt("unread_count"));
                session.setStatus(rs.getInt("status"));
                Timestamp createdAt = rs.getTimestamp("created_at");
                Timestamp updatedAt = rs.getTimestamp("updated_at");
                session.setCreateTime(createdAt != null ? createdAt.getTime() : 0);
                session.setUpdateTime(updatedAt != null ? updatedAt.getTime() : 0);
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
    public void updateSessionTitleAsync(long sessionId, String title, UpdateSessionCallback callback) {
        new Thread(() -> {
            boolean success = updateSessionTitle(sessionId, title);
            if (callback != null) {
                callback.onResult(success);
            }
        }).start();
    }
    
    /**
     * 删除用户的所有会话（软删除）
     */
    public boolean deleteAllSessions(String userName) {
        boolean flag = false;
        String sql = "UPDATE chat_sessions cs " +
                     "INNER JOIN user_info ui ON cs.user_id = ui.user_id " +
                     "SET cs.status = 0, cs.updated_at = NOW() " +
                     "WHERE ui.username = ?";
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

    // ========== 回调接口 ==========
    
    public interface CreateSessionCallback {
        void onResult(long sessionId);
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