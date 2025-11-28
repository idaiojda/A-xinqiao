package com.example.xinqiao.mysql;

import android.content.ContentValues;
import android.content.Context;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import android.content.SharedPreferences;
import android.text.TextUtils;
import com.example.xinqiao.util.AnalysisUtils;
import com.example.xinqiao.bean.UserBean;
import com.example.xinqiao.bean.VideoBean;

public class DBUtils {
    private static DBUtils instance;
    private MySQLHelper helper;
    
    private DBUtils(Context context) {
        // 构造函数不再抛出异常，延迟初始化
    }
    
    public static void init(Context context, final InitCallback callback) {
        if (instance == null) {
            instance = new DBUtils(context);
        }
        
        MySQLHelper.init(context, new MySQLHelper.InitCallback() {
            @Override
            public void onSuccess() {
                instance.helper = MySQLHelper.getInstance();
                if (callback != null) {
                    callback.onSuccess();
                }
            }
            
            @Override
            public void onError(SQLException e) {
                android.util.Log.e("DBUtils", "数据库初始化失败: " + e.getMessage());
                instance.helper = null;
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    public interface InitCallback {
        void onSuccess();
        void onError(SQLException e);
    }
    
    public static DBUtils getInstance(Context context) throws SQLException {
        if (instance == null) {
            instance = new DBUtils(context);
        }
        return instance;
    }
    
    /**
     * 检查数据库连接是否可用
     */
    public boolean isDatabaseAvailable() {
        return helper != null;
    }
    
    // 保存用户信息
    public boolean saveUserInfo(UserBean userBean) {
        return saveUserInfo(userBean.userName, userBean.password,
                userBean.nickName, userBean.sex, userBean.signature, userBean.avatarPath);
    }
    
    // 保存用户信息
    public boolean saveUserInfo(String userName, String password,
            String nickName, String sex, String signature, String avatarPath) {
        Connection conn = null;
        try {
            conn = helper.getConnection();
            String sql = "INSERT INTO user_info (username, password, nickname, gender, introduction, avatar, balance, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW());";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, userName);
            stmt.setString(2, password);
            stmt.setString(3, nickName);
            stmt.setString(4, sex);
            stmt.setString(5, signature);
            stmt.setNull(6, java.sql.Types.BLOB);
            stmt.setDouble(7, 0.00); // 设置初始余额为0
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            android.util.Log.e("DBUtils", "保存用户信息失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                helper.releaseConnection(conn);
            }
        }
    }
    
    // 更新用户信息
    public boolean updateUserInfo(String userName, String nickName,
            String sex, String signature, String avatarPath) {
        Connection conn = null;
        try {
            conn = helper.getConnection();
            String sql = "UPDATE user_info SET nickname=?, gender=?, introduction=?, updated_at=NOW() WHERE username=?;";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, nickName);
            stmt.setString(2, sex);
            stmt.setString(3, signature);
            stmt.setString(4, userName);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            android.util.Log.e("DBUtils", "更新用户信息失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                helper.releaseConnection(conn);
            }
        }
    }
    
    /**
     * @deprecated 使用 {@link #validateUser(String, String)} 代替
     */
    @Deprecated
    public boolean userLogin(String userName, String password) {
        return validateUser(userName, password) != -1;
    }
    
    // 验证用户登录信息
    public int validateUser(String userName, String password) {
        Connection conn = null;
        try {
            conn = helper.getConnection();
            String sql = "SELECT user_id FROM user_info WHERE username=? AND password=?;";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, userName);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int userId = rs.getInt("user_id");
                rs.close();
                stmt.close();
                return userId;
            } else {
                rs.close();
                stmt.close();
                return -1; // 验证失败
            }
        } catch (SQLException e) {
            android.util.Log.e("DBUtils", "验证用户失败: " + e.getMessage());
            return -1; // 发生异常
        } finally {
            if (conn != null) {
                helper.releaseConnection(conn);
            }
        }
    }
    
    // 保存播放记录
    public interface SavePlayListCallback {
        void onResult(boolean success);
    }
    
    public void saveVideoPlayList(VideoBean videoBean, String userName, final SavePlayListCallback callback) {
        saveVideoPlayList(userName, videoBean.chapterId, videoBean.videoId,
                videoBean.videoPath, videoBean.title, videoBean.secondTitle, callback);
    }
    
    /**
     * @deprecated 使用 {@link #saveVideoPlayList(VideoBean, String, SavePlayListCallback)} 代替
     */
    @Deprecated
    public boolean saveVideoPlayList(VideoBean videoBean, String userName) {
        return saveVideoPlayList(userName, videoBean.chapterId, videoBean.videoId,
                videoBean.videoPath, videoBean.title, videoBean.secondTitle);
    }

    // 保存播放记录（异步方法）
    public void saveVideoPlayList(String userName, int chapterId, int videoId,
            String videoPath, String title, String secondTitle, final SavePlayListCallback callback) {
        if (helper == null) {
            android.util.Log.e("DBUtils", "数据库连接未初始化");
            android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            mainHandler.post(() -> callback.onResult(false));
            return;
        }
        
        new Thread(() -> {
            helper.getConnection(new MySQLHelper.ConnectionResultCallback() {
                @Override
                public void onSuccess(Connection conn) {
                    try {
                        // 先删除旧记录，保证同一视频只有一条最新记录
                        String delSql = "DELETE FROM videoplaylist WHERE userName=? AND chapterId=? AND videoId=?";
                        PreparedStatement delStmt = conn.prepareStatement(delSql);
                        delStmt.setString(1, userName);
                        delStmt.setInt(2, chapterId);
                        delStmt.setInt(3, videoId);
                        delStmt.executeUpdate();

                        long now = System.currentTimeMillis();
                        boolean success;
                        try {
                            // 带播放时间戳的插入（新表结构）
                            String insertSql = "INSERT INTO videoplaylist (userName, chapterId, videoId, videoPath, title, secondTitle, playTimestamp) VALUES (?, ?, ?, ?, ?, ?, ?)";
                            PreparedStatement stmt = conn.prepareStatement(insertSql);
                            stmt.setString(1, userName);
                            stmt.setInt(2, chapterId);
                            stmt.setInt(3, videoId);
                            stmt.setString(4, videoPath);
                            stmt.setString(5, title);
                            stmt.setString(6, secondTitle);
                            stmt.setLong(7, now);
                            success = stmt.executeUpdate() > 0;
                        } catch (SQLException ie) {
                            // 老表结构兼容插入（无playTimestamp列）
                            android.util.Log.w("DBUtils", "插入包含播放时间戳失败，降级到旧结构: " + ie.getMessage());
                            String insertSqlLegacy = "INSERT INTO videoplaylist (userName, chapterId, videoId, videoPath, title, secondTitle) VALUES (?, ?, ?, ?, ?, ?)";
                            PreparedStatement stmt = conn.prepareStatement(insertSqlLegacy);
                            stmt.setString(1, userName);
                            stmt.setInt(2, chapterId);
                            stmt.setInt(3, videoId);
                            stmt.setString(4, videoPath);
                            stmt.setString(5, title);
                            stmt.setString(6, secondTitle);
                            success = stmt.executeUpdate() > 0;
                        }
                        
                        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                        final boolean successFinal = success;
                        mainHandler.post(() -> callback.onResult(successFinal));
                    } catch (SQLException e) {
                        e.printStackTrace();
                        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                        mainHandler.post(() -> callback.onResult(false));
                    } finally {
                        helper.releaseConnection(conn);
                    }
                }
                
                @Override
                public void onError(SQLException e) {
                    android.util.Log.e("DBUtils", "获取连接失败: " + e.getMessage());
                    android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                    mainHandler.post(() -> callback.onResult(false));
                }
            });
        }).start();
    }
    
    /**
     * @deprecated 使用 {@link #saveVideoPlayList(String, int, int, String, String, String, SavePlayListCallback)} 代替
     */
    @Deprecated
    public boolean saveVideoPlayList(String userName, int chapterId, int videoId,
            String videoPath, String title, String secondTitle) {
        if (helper == null) {
            android.util.Log.e("DBUtils", "数据库连接未初始化");
            return false;
        }
        
        Connection conn = null;
        try {
            conn = helper.getConnection();
            if (conn == null) {
                android.util.Log.e("DBUtils", "无法获取数据库连接");
                return false;
            }
            // 先删除旧记录，再插入新记录（尽量写入时间戳）
            String delSql = "DELETE FROM videoplaylist WHERE userName=? AND chapterId=? AND videoId=?";
            PreparedStatement delStmt = conn.prepareStatement(delSql);
            delStmt.setString(1, userName);
            delStmt.setInt(2, chapterId);
            delStmt.setInt(3, videoId);
            delStmt.executeUpdate();

            long now = System.currentTimeMillis();
            try {
                String insertSql = "INSERT INTO videoplaylist (userName, chapterId, videoId, videoPath, title, secondTitle, playTimestamp) VALUES (?, ?, ?, ?, ?, ?, ?)";
                PreparedStatement stmt = conn.prepareStatement(insertSql);
                stmt.setString(1, userName);
                stmt.setInt(2, chapterId);
                stmt.setInt(3, videoId);
                stmt.setString(4, videoPath);
                stmt.setString(5, title);
                stmt.setString(6, secondTitle);
                stmt.setLong(7, now);
                return stmt.executeUpdate() > 0;
            } catch (SQLException ie) {
                android.util.Log.w("DBUtils", "插入包含播放时间戳失败，降级到旧结构: " + ie.getMessage());
                String insertSqlLegacy = "INSERT INTO videoplaylist (userName, chapterId, videoId, videoPath, title, secondTitle) VALUES (?, ?, ?, ?, ?, ?)";
                PreparedStatement stmt = conn.prepareStatement(insertSqlLegacy);
                stmt.setString(1, userName);
                stmt.setInt(2, chapterId);
                stmt.setInt(3, videoId);
                stmt.setString(4, videoPath);
                stmt.setString(5, title);
                stmt.setString(6, secondTitle);
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                helper.releaseConnection(conn);
            }
        }
    }
    
    // 删除播放记录
    public interface DeleteHistoryCallback {
        void onResult(boolean success);
    }
    
    public void deleteVideoPlayList(String userName, int chapterId, int videoId, final DeleteHistoryCallback callback) {
        if (helper == null) {
            android.util.Log.e("DBUtils", "数据库连接未初始化");
            android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            mainHandler.post(() -> callback.onResult(false));
            return;
        }
        
        new Thread(() -> {
            helper.getConnection(new MySQLHelper.ConnectionResultCallback() {
                @Override
                public void onSuccess(Connection conn) {
                    try {
                        String sql = "DELETE FROM videoplaylist WHERE userName=? AND chapterId=? AND videoId=?;";
                        PreparedStatement stmt = conn.prepareStatement(sql);
                        stmt.setString(1, userName);
                        stmt.setInt(2, chapterId);
                        stmt.setInt(3, videoId);
                        boolean success = stmt.executeUpdate() > 0;
                        
                        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                        mainHandler.post(() -> callback.onResult(success));
                    } catch (SQLException e) {
                        e.printStackTrace();
                        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                        mainHandler.post(() -> callback.onResult(false));
                    } finally {
                        helper.releaseConnection(conn);
                    }
                }
                
                @Override
                public void onError(SQLException e) {
                    android.util.Log.e("DBUtils", "获取连接失败: " + e.getMessage());
                    android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                    mainHandler.post(() -> callback.onResult(false));
                }
            });
        }).start();
    }
    
    /**
     * @deprecated 使用 {@link #deleteVideoPlayList(String, int, int, DeleteHistoryCallback)} 代替
     */
    @Deprecated
    public boolean deleteVideoPlayList(String userName, int chapterId, int videoId) {
        if (helper == null) {
            android.util.Log.e("DBUtils", "数据库连接未初始化");
            return false;
        }
        
        try {
            Connection conn = helper.getConnection();
            String sql = "DELETE FROM videoplaylist WHERE userName=? AND chapterId=? AND videoId=?;";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, userName);
            stmt.setInt(2, chapterId);
            stmt.setInt(3, videoId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            helper.closeConnection();
        }
    }
    
    // 保存聊天记录
    public boolean saveChatHistory(String userName, String content, int type, long timestamp) {
        Connection conn = null;
        try {
            conn = helper.getConnection();
            String sql = "INSERT INTO chat_history (userName, content, type, timestamp) VALUES (?, ?, ?, ?);";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, userName);
            stmt.setString(2, content);
            stmt.setInt(3, type);
            stmt.setLong(4, timestamp);
            boolean result = stmt.executeUpdate() > 0;
            stmt.close();
            return result;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                helper.releaseConnection(conn);
            }
        }
    }
    
    // 保存文章阅读记录
    public boolean saveArticleHistory(String userName, int articleId, String title,
            String content, String category, long readTimestamp, int readProgress) {
        Connection conn = null;
        try {
            conn = helper.getConnection();
            String sql = "INSERT INTO article_history (userName, articleId, title, content, category, readTimestamp, readProgress) VALUES (?, ?, ?, ?, ?, ?, ?);";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, userName);
            stmt.setInt(2, articleId);
            stmt.setString(3, title);
            stmt.setString(4, content);
            stmt.setString(5, category);
            stmt.setLong(6, readTimestamp);
            stmt.setInt(7, readProgress);
            boolean result = stmt.executeUpdate() > 0;
            stmt.close();
            return result;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                helper.releaseConnection(conn);
            }
        }
    }

    // 获取用户头像路径（现在是获取二进制数据）
    public void getUserAvatarPath(String userName, final AvatarPathCallback callback) {
        if (helper == null) {
            android.util.Log.e("DBUtils", "数据库连接未初始化");
            android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            mainHandler.post(() -> { if (callback != null) callback.onSuccess(null); });
            return;
        }
        new Thread(() -> {
            helper.getConnection(new MySQLHelper.ConnectionResultCallback() {
                @Override
                public void onSuccess(Connection conn) {
                    try {
                        String sql = "SELECT avatar FROM user_info WHERE username=?;";
                        PreparedStatement stmt = conn.prepareStatement(sql);
                        stmt.setString(1, userName);
                        ResultSet rs = stmt.executeQuery();
                        if (rs.next()) {
                            final byte[] avatarData = rs.getBytes("avatar");
                            android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                            
                            // 检查图片大小，如果过大则不进行Base64编码
                            if (avatarData != null) {
                                // 如果图片数据超过2MB，则不进行Base64编码，直接返回null
                                if (avatarData.length > 2 * 1024 * 1024) {
                                    mainHandler.post(() -> callback.onSuccess(null));
                                    return;
                                }
                                
                                // 使用异步任务处理Base64编码，避免阻塞主线程
                                new Thread(() -> {
                                    try {
                                        // 分段处理Base64编码，避免一次性创建大字符串
                                        String base64Data = android.util.Base64.encodeToString(avatarData, android.util.Base64.DEFAULT);
                                        
                                        // 在主线程返回结果
                                        mainHandler.post(() -> {
                                            try {
                                                // 添加弱引用检查，避免在Activity销毁后执行回调
                                                if (callback instanceof WeakReferenceCallback) {
                                                    if (((WeakReferenceCallback) callback).isAlive()) {
                                                        callback.onSuccess("data:image/jpeg;base64," + base64Data);
                                                    }
                                                } else {
                                                    callback.onSuccess("data:image/jpeg;base64," + base64Data);
                                                }
                                            } catch (OutOfMemoryError e) {
                                                android.util.Log.e("DBUtils", "Base64编码内存溢出: " + e.getMessage());
                                                callback.onSuccess(null);
                                            }
                                        });
                                    } catch (OutOfMemoryError e) {
                                        android.util.Log.e("DBUtils", "Base64编码内存溢出: " + e.getMessage());
                                        mainHandler.post(() -> callback.onSuccess(null));
                                    }
                                }).start();
                            } else {
                                mainHandler.post(() -> callback.onSuccess(null));
                            }
                        } else {
                            android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                            mainHandler.post(() -> callback.onSuccess(null));
                        }
                    } catch (SQLException e) {
                        android.util.Log.e("DBUtils", "getUserAvatar: 查询异常: " + e.getMessage());
                        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                        mainHandler.post(() -> callback.onError(e));
                    } finally {
                        helper.releaseConnection(conn);
                    }
                }
                @Override
                public void onError(SQLException e) {
                    android.util.Log.e("DBUtils", "getUserAvatar: 获取连接失败: " + e.getMessage());
                    android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                    mainHandler.post(() -> callback.onError(e));
                }
            });
        }).start();
    }

    public interface AvatarPathCallback {
        void onSuccess(String avatarBase64);
        void onError(SQLException e);
    }

    // 更新用户头像（现在是更新二进制数据）
    public void updateUserAvatar(String userName, byte[] avatarData, final UpdateAvatarCallback callback) {
        if (helper == null) {
            android.util.Log.e("DBUtils", "数据库连接未初始化");
            android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            mainHandler.post(() -> { if (callback != null) callback.onResult(false); });
            return;
        }
        new Thread(() -> {
            helper.getConnection(new MySQLHelper.ConnectionResultCallback() {
                @Override
                public void onSuccess(Connection conn) {
                    try {
                        String sql = "UPDATE user_info SET avatar = ?, updated_at = NOW() WHERE username = ?;";
                        PreparedStatement stmt = conn.prepareStatement(sql);
                        stmt.setBytes(1, avatarData);
                        stmt.setString(2, userName);
                        int rowsAffected = stmt.executeUpdate();
                        android.util.Log.d("DBUtils", "更新用户头像结果: " + rowsAffected + "行受影响");
                        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                        mainHandler.post(() -> callback.onResult(rowsAffected > 0));
                    } catch (SQLException e) {
                        android.util.Log.e("DBUtils", "updateUserAvatar: 更新异常: " + e.getMessage());
                        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                        mainHandler.post(() -> callback.onResult(false));
                    } finally {
                        helper.releaseConnection(conn);
                    }
                }
                @Override
                public void onError(SQLException e) {
                    android.util.Log.e("DBUtils", "updateUserAvatar: 获取连接失败: " + e.getMessage());
                    android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                    mainHandler.post(() -> callback.onResult(false));
                }
            });
        }).start();
    }

    public interface UpdateAvatarCallback {
        void onResult(boolean success);
    }

    // 清除用户数据
    public boolean clearUserData() {
        Connection conn = null;
        try {
            conn = helper.getConnection();
            String[] tables = {"user_info", "videoplaylist", "chat_history", "article_history"};
            for (String table : tables) {
                String sql = "DELETE FROM " + table;
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.executeUpdate();
                stmt.close();
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                helper.releaseConnection(conn);
            }
        }
    }

    // 获取当前用户名（优先从统一的loginInfo读取，其次按用户ID回查数据库）
    public void getCurrentUserName(Context context, final UserNameCallback callback) {
        // 统一使用 AnalysisUtils 从 "loginInfo" 读取登录用户名
        String userName = AnalysisUtils.readLoginUserName(context);

        if (!TextUtils.isEmpty(userName)) {
            callback.onSuccess(userName);
            return;
        }

        // 若用户名为空，则尝试读取已保存的用户ID并从数据库回查用户名
        final int userId = AnalysisUtils.readUserId(context);
        if (userId == -1) {
            android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            mainHandler.post(() -> callback.onSuccess(null));
            return;
        }

        new Thread(() -> {
            helper.getConnection(new MySQLHelper.ConnectionResultCallback() {
                @Override
                public void onSuccess(Connection conn) {
                    try {
                        String sql = "SELECT username FROM user_info WHERE user_id = ?;";
                        PreparedStatement stmt = conn.prepareStatement(sql);
                        stmt.setInt(1, userId);
                        ResultSet rs = stmt.executeQuery();
                        if (rs.next()) {
                            final String fetchedUserName = rs.getString("username");
                            android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                            mainHandler.post(() -> callback.onSuccess(fetchedUserName));
                        } else {
                            android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                            mainHandler.post(() -> callback.onSuccess(null));
                        }
                    } catch (SQLException e) {
                        android.util.Log.e("DBUtils", "getCurrentUserName: 查询异常: " + e.getMessage());
                        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                        mainHandler.post(() -> callback.onError(e));
                    } finally {
                        helper.releaseConnection(conn);
                    }
                }
                @Override
                public void onError(SQLException e) {
                    android.util.Log.e("DBUtils", "getCurrentUserName: 获取连接失败: " + e.getMessage());
                    android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                    mainHandler.post(() -> callback.onError(e));
                }
            });
        }).start();
    }

    public interface UserNameCallback {
        void onSuccess(String userName);
        void onError(SQLException e);
    }

    // 获取用户ID
    public void getUserId(String userName, final UserIdCallback callback) {
        new Thread(() -> {
            helper.getConnection(new MySQLHelper.ConnectionResultCallback() {
                @Override
                public void onSuccess(Connection conn) {
                    try {
                        String sql = "SELECT user_id FROM user_info WHERE username = ?;";
                        PreparedStatement stmt = conn.prepareStatement(sql);
                        stmt.setString(1, userName);
                        ResultSet rs = stmt.executeQuery();
                        if (rs.next()) {
                            final int userId = rs.getInt("user_id");
                            android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                            mainHandler.post(() -> callback.onSuccess(userId));
                        } else {
                            android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                            mainHandler.post(() -> callback.onSuccess(-1));
                        }
                    } catch (SQLException e) {
                        android.util.Log.e("DBUtils", "getUserId: 查询异常: " + e.getMessage());
                        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                        mainHandler.post(() -> callback.onError(e));
                    } finally {
                        helper.releaseConnection(conn);
                    }
                }
                @Override
                public void onError(SQLException e) {
                    android.util.Log.e("DBUtils", "getUserId: 获取连接失败: " + e.getMessage());
                    android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                    mainHandler.post(() -> callback.onError(e));
                }
            });
        }).start();
    }

    public interface UserIdCallback {
        void onSuccess(int userId);
        void onError(SQLException e);
    }

    // 获取用户昵称
    public void getUserNickname(String userName, final UserNicknameCallback callback) {
        new Thread(() -> {
            helper.getConnection(new MySQLHelper.ConnectionResultCallback() {
                @Override
                public void onSuccess(Connection conn) {
                    try {
                        String sql = "SELECT nickname FROM user_info WHERE username = ?;";
                        PreparedStatement stmt = conn.prepareStatement(sql);
                        stmt.setString(1, userName);
                        ResultSet rs = stmt.executeQuery();
                        if (rs.next()) {
                            final String nickname = rs.getString("nickname");
                            android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                            mainHandler.post(() -> callback.onSuccess(nickname));
                        } else {
                            android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                            mainHandler.post(() -> callback.onSuccess(null));
                        }
                    } catch (SQLException e) {
                        android.util.Log.e("DBUtils", "getUserNickname: 查询异常: " + e.getMessage());
                        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                        mainHandler.post(() -> callback.onError(e));
                    } finally {
                        helper.releaseConnection(conn);
                    }
                }
                @Override
                public void onError(SQLException e) {
                    android.util.Log.e("DBUtils", "getUserNickname: 获取连接失败: " + e.getMessage());
                    android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                    mainHandler.post(() -> callback.onError(e));
                }
            });
        }).start();
    }

    public interface UserNicknameCallback {
        void onSuccess(String nickname);
        void onError(SQLException e);
    }

    public String getUserAvatarPathSync(String userName) throws SQLException {
        Connection conn = null;
        try {
            conn = MySQLHelper.getInstance().getConnection();
            String sql = "SELECT avatar FROM user_info WHERE username=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, userName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                byte[] bytes = rs.getBytes("avatar");
                rs.close(); stmt.close();
                if (bytes != null && bytes.length > 0) {
                    String b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP);
                    return "data:image/jpeg;base64," + b64;
                }
            } else { rs.close(); stmt.close(); }
            return null;
        } catch (Exception e) {
            android.util.Log.e("DBUtils", "getUserAvatarPathSync error: " + e.getMessage());
            throw new SQLException(e.getMessage());
        } finally {
            if (conn != null) MySQLHelper.getInstance().releaseConnection(conn);
        }
    }

    /**
     * 同步获取用户头像（用户名或昵称均可）
     */
    public String getUserAvatarPathByNameOrNickSync(String nameOrNick) throws SQLException {
        Connection conn = null;
        try {
            conn = MySQLHelper.getInstance().getConnection();
            String sql = "SELECT avatar FROM user_info WHERE username=? OR nickname=? LIMIT 1";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, nameOrNick);
            stmt.setString(2, nameOrNick);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                byte[] bytes = rs.getBytes("avatar");
                rs.close(); stmt.close();
                if (bytes != null && bytes.length > 0) {
                    String b64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP);
                    return "data:image/jpeg;base64," + b64;
                }
            } else { rs.close(); stmt.close(); }
            return null;
        } catch (Exception e) {
            android.util.Log.e("DBUtils", "getUserAvatarPathByNameOrNickSync error: " + e.getMessage());
            throw new SQLException(e.getMessage());
        } finally {
            if (conn != null) MySQLHelper.getInstance().releaseConnection(conn);
        }
    }

    /**
     * 同步获取用户昵称
     */
    public String getUserNicknameSync(String userName) throws SQLException {
        Connection conn = null;
        try {
            conn = MySQLHelper.getInstance().getConnection();
            String sql = "SELECT nickname FROM user_info WHERE username=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, userName);
            ResultSet rs = stmt.executeQuery();
            String nickname = null;
            if (rs.next()) {
                nickname = rs.getString("nickname");
            }
            rs.close(); stmt.close();
            return nickname;
        } catch (Exception e) {
            throw new SQLException(e.getMessage());
        } finally {
            if (conn != null) MySQLHelper.getInstance().releaseConnection(conn);
        }
    }

    public List<String> listUserNamesByKeyword(String keyword) {
        List<String> list = new ArrayList<>();
        Connection conn = null;
        try {
            conn = MySQLHelper.getInstance().getConnection();
            String sql = "SELECT username FROM user_info WHERE username LIKE ? OR nickname LIKE ? ORDER BY updated_at DESC LIMIT 50";
            PreparedStatement stmt = conn.prepareStatement(sql);
            String like = "%" + (keyword == null ? "" : keyword) + "%";
            stmt.setString(1, like);
            stmt.setString(2, like);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(rs.getString("username"));
            }
            rs.close(); stmt.close();
        } catch (Exception e) {
            android.util.Log.e("DBUtils", "listUserNamesByKeyword error: " + e.getMessage());
        } finally {
            if (conn != null) MySQLHelper.getInstance().releaseConnection(conn);
        }
        return list;
    }

    public byte[] getUserAvatarBytesSync(String userName) throws SQLException {
        Connection conn = null;
        try {
            conn = MySQLHelper.getInstance().getConnection();
            String sql = "SELECT avatar FROM user_info WHERE username=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, userName);
            ResultSet rs = stmt.executeQuery();
            byte[] bytes = null;
            if (rs.next()) {
                bytes = rs.getBytes("avatar");
            }
            rs.close(); stmt.close();
            return bytes;
        } catch (Exception e) {
            android.util.Log.e("DBUtils", "getUserAvatarBytesSync error: " + e.getMessage());
            throw new SQLException(e.getMessage());
        } finally {
            if (conn != null) MySQLHelper.getInstance().releaseConnection(conn);
        }
    }

    // --- 社区 · 群聊与小组 ---
    public static class GroupMessageRecord {
        public String id;
        public String groupName;
        public String author;
        public String authorAvatar;
        public String content;
        public String imagesJson;
        public String mentionsJson;
        public String voiceUrl;
        public Integer voiceDurationSec;
        public long timestamp;
        public boolean recalled;
    }

    public static class GroupInfoRow {
        public String name;
        public String description;
        public String adminName;
        public String schedule;
        public int capacity;
        public int memberCount;
        public String rulesJson;
    }

    public List<GroupMessageRecord> listCommunityGroupMessages(String groupName) {
        List<GroupMessageRecord> list = new ArrayList<>();
        Connection conn = null;
        try {
            conn = MySQLHelper.getInstance().getConnection();
            String sql = "SELECT * FROM community_group_messages WHERE group_name=? ORDER BY timestamp ASC";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, groupName);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                GroupMessageRecord r = new GroupMessageRecord();
                r.id = rs.getString("id");
                r.groupName = rs.getString("group_name");
                r.author = rs.getString("author");
                try {
                    byte[] av = rs.getBytes("author_avatar");
                    if (av != null && av.length > 0) {
                        String b64 = android.util.Base64.encodeToString(av, android.util.Base64.NO_WRAP);
                        r.authorAvatar = "data:image/jpeg;base64," + b64;
                    } else {
                        r.authorAvatar = null;
                    }
                } catch (Exception ignore) { r.authorAvatar = null; }
                r.content = rs.getString("content");
                r.imagesJson = rs.getString("images_json");
                r.mentionsJson = rs.getString("mentions_json");
                r.voiceUrl = rs.getString("voice_url");
                r.voiceDurationSec = rs.getObject("voice_duration_sec") != null ? rs.getInt("voice_duration_sec") : null;
                r.timestamp = rs.getLong("timestamp");
                r.recalled = rs.getInt("recalled") == 1;
                list.add(r);
            }
            rs.close(); stmt.close();
        } catch (Exception e) {
            android.util.Log.e("DBUtils", "listCommunityGroupMessages error: " + e.getMessage());
        } finally {
            if (conn != null) MySQLHelper.getInstance().releaseConnection(conn);
        }
        return list;
    }

    public void markVoiceRead(String groupName, String userName, String messageId) {
        Connection conn = null;
        try {
            conn = MySQLHelper.getInstance().getConnection();
            conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS community_group_message_reads (" +
                "message_id VARCHAR(64) NOT NULL, " +
                "user_name VARCHAR(50) NOT NULL, " +
                "read_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "PRIMARY KEY (message_id, user_name)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"
            );
            String sql = "INSERT INTO community_group_message_reads (message_id, user_name, read_at) VALUES (?, ?, CURRENT_TIMESTAMP) ON DUPLICATE KEY UPDATE read_at = CURRENT_TIMESTAMP";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, messageId);
            stmt.setString(2, userName);
            stmt.executeUpdate();
            stmt.close();
        } catch (Exception e) {
            android.util.Log.e("DBUtils", "markVoiceRead error: " + e.getMessage());
        } finally {
            if (conn != null) MySQLHelper.getInstance().releaseConnection(conn);
        }
    }

    public List<String> listVoiceRead(String groupName, String userName) {
        List<String> ids = new ArrayList<>();
        Connection conn = null;
        try {
            conn = MySQLHelper.getInstance().getConnection();
            conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS community_group_message_reads (" +
                "message_id VARCHAR(64) NOT NULL, " +
                "user_name VARCHAR(50) NOT NULL, " +
                "read_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "PRIMARY KEY (message_id, user_name)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"
            );
            String sql = "SELECT r.message_id FROM community_group_message_reads r " +
                         "JOIN community_group_messages m ON r.message_id = m.id " +
                         "WHERE m.group_name = ? AND r.user_name = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, groupName);
            stmt.setString(2, userName);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) { ids.add(rs.getString("message_id")); }
            rs.close(); stmt.close();
        } catch (Exception e) {
            android.util.Log.e("DBUtils", "listVoiceRead error: " + e.getMessage());
        } finally {
            if (conn != null) MySQLHelper.getInstance().releaseConnection(conn);
        }
        return ids;
    }

    public GroupMessageRecord insertCommunityGroupMessage(GroupMessageRecord msg) {
        Connection conn = null;
        try {
            if (msg.id == null || msg.id.isEmpty()) msg.id = "gm" + System.currentTimeMillis();
            conn = MySQLHelper.getInstance().getConnection();
            try {
                String ensureGroup = "INSERT INTO community_groups (name) VALUES (?) ON DUPLICATE KEY UPDATE name = VALUES(name)";
                PreparedStatement eg = conn.prepareStatement(ensureGroup);
                eg.setString(1, msg.groupName);
                eg.executeUpdate();
                eg.close();
            } catch (Exception e) {
                android.util.Log.w("DBUtils", "ensure group failed: " + e.getMessage());
            }
            try {
                setCommunityGroupJoin(msg.groupName, msg.author, true);
            } catch (Exception e) {
                android.util.Log.w("DBUtils", "ensure member failed: " + e.getMessage());
            }
            try {
                // 尝试扩大列容量以容纳头像二进制（如历史版本为VARCHAR）
                conn.createStatement().execute("ALTER TABLE community_group_messages MODIFY author_avatar LONGBLOB");
            } catch (Exception ignore) {}
            String sql = "INSERT INTO community_group_messages (id, group_name, author, author_avatar, content, images_json, mentions_json, voice_url, voice_duration_sec, timestamp, recalled) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, msg.id);
            stmt.setString(2, msg.groupName);
            stmt.setString(3, msg.author);
            try {
                byte[] avBytes = null;
                try {
                    // 先按用户名取，失败则按昵称取
                    avBytes = getUserAvatarBytesSync(msg.author);
                } catch (Exception ignored) {}
                if (avBytes == null || avBytes.length == 0) {
                    try {
                        Connection c2 = MySQLHelper.getInstance().getConnection();
                        String sql2 = "SELECT avatar FROM user_info WHERE nickname=? LIMIT 1";
                        PreparedStatement s2 = c2.prepareStatement(sql2);
                        s2.setString(1, msg.author);
                        ResultSet r2 = s2.executeQuery();
                        if (r2.next()) { avBytes = r2.getBytes("avatar"); }
                        r2.close(); s2.close();
                        MySQLHelper.getInstance().releaseConnection(c2);
                    } catch (Exception ignored) {}
                }
                if (avBytes != null && avBytes.length > 0) stmt.setBytes(4, avBytes); else stmt.setNull(4, java.sql.Types.BLOB);
            } catch (Exception e) { stmt.setNull(4, java.sql.Types.BLOB); }
            stmt.setString(5, msg.content);
            stmt.setString(6, msg.imagesJson);
            stmt.setString(7, msg.mentionsJson);
            stmt.setString(8, msg.voiceUrl);
            if (msg.voiceDurationSec == null) stmt.setNull(9, java.sql.Types.INTEGER); else stmt.setInt(9, msg.voiceDurationSec);
            stmt.setLong(10, msg.timestamp);
            stmt.setInt(11, msg.recalled ? 1 : 0);
            stmt.executeUpdate();
            stmt.close();
            return msg;
        } catch (Exception e) {
            android.util.Log.e("DBUtils", "insertCommunityGroupMessage error: " + e.getMessage());
            return null;
        } finally {
            if (conn != null) MySQLHelper.getInstance().releaseConnection(conn);
        }
    }

    public boolean recallCommunityGroupMessage(String id) {
        Connection conn = null;
        try {
            conn = MySQLHelper.getInstance().getConnection();
            String sql = "UPDATE community_group_messages SET recalled=1 WHERE id=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, id);
            int rows = stmt.executeUpdate();
            stmt.close();
            return rows > 0;
        } catch (Exception e) {
            android.util.Log.e("DBUtils", "recallCommunityGroupMessage error: " + e.getMessage());
            return false;
        } finally {
            if (conn != null) MySQLHelper.getInstance().releaseConnection(conn);
        }
    }

    public boolean deleteCommunityGroupMessage(String id) {
        Connection conn = null;
        try {
            conn = MySQLHelper.getInstance().getConnection();
            String sql = "DELETE FROM community_group_messages WHERE id=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, id);
            int rows = stmt.executeUpdate();
            stmt.close();
            return rows > 0;
        } catch (Exception e) {
            android.util.Log.e("DBUtils", "deleteCommunityGroupMessage error: " + e.getMessage());
            return false;
        } finally {
            if (conn != null) MySQLHelper.getInstance().releaseConnection(conn);
        }
    }

    public int deleteCommunityGroupMessagesByGroup(String groupName) {
        Connection conn = null;
        try {
            conn = MySQLHelper.getInstance().getConnection();
            String sql = "DELETE FROM community_group_messages WHERE group_name=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, groupName);
            int rows = stmt.executeUpdate();
            stmt.close();
            return rows;
        } catch (Exception e) {
            android.util.Log.e("DBUtils", "deleteCommunityGroupMessagesByGroup error: " + e.getMessage());
            return 0;
        } finally {
            if (conn != null) MySQLHelper.getInstance().releaseConnection(conn);
        }
    }

    public int deleteVoiceReadForUser(String groupName, String userName) {
        Connection conn = null;
        try {
            conn = MySQLHelper.getInstance().getConnection();
            conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS community_group_message_reads (" +
                "message_id VARCHAR(64) NOT NULL, " +
                "user_name VARCHAR(50) NOT NULL, " +
                "read_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "PRIMARY KEY (message_id, user_name)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"
            );
            String sql = "DELETE r FROM community_group_message_reads r " +
                         "JOIN community_group_messages m ON r.message_id = m.id " +
                         "WHERE m.group_name = ? AND r.user_name = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, groupName);
            stmt.setString(2, userName);
            int rows = stmt.executeUpdate();
            stmt.close();
            return rows;
        } catch (Exception e) {
            android.util.Log.e("DBUtils", "deleteVoiceReadForUser error: " + e.getMessage());
            return 0;
        } finally {
            if (conn != null) MySQLHelper.getInstance().releaseConnection(conn);
        }
    }

    public boolean upsertCommunityGroup(GroupInfoRow info) {
        Connection conn = null;
        try {
            conn = MySQLHelper.getInstance().getConnection();
            String sql = "INSERT INTO community_groups (name, description, admin_name, schedule, capacity, member_count, rules_json) VALUES (?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE description=VALUES(description), admin_name=VALUES(admin_name), schedule=VALUES(schedule), capacity=VALUES(capacity), rules_json=VALUES(rules_json)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, info.name);
            stmt.setString(2, info.description);
            stmt.setString(3, info.adminName);
            stmt.setString(4, info.schedule);
            stmt.setInt(5, info.capacity);
            stmt.setInt(6, info.memberCount);
            stmt.setString(7, info.rulesJson);
            int rows = stmt.executeUpdate();
            stmt.close();
            return rows > 0;
        } catch (Exception e) {
            android.util.Log.e("DBUtils", "upsertCommunityGroup error: " + e.getMessage());
            return false;
        } finally {
            if (conn != null) MySQLHelper.getInstance().releaseConnection(conn);
        }
    }

    public GroupInfoRow getCommunityGroupInfo(String name) {
        Connection conn = null;
        try {
            conn = MySQLHelper.getInstance().getConnection();
            String sql = "SELECT * FROM community_groups WHERE name=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                GroupInfoRow row = new GroupInfoRow();
                row.name = rs.getString("name");
                row.description = rs.getString("description");
                row.adminName = rs.getString("admin_name");
                row.schedule = rs.getString("schedule");
                row.capacity = rs.getInt("capacity");
                row.memberCount = rs.getInt("member_count");
                row.rulesJson = rs.getString("rules_json");
                rs.close(); stmt.close();
                return row;
            }
            rs.close(); stmt.close();
            return null;
        } catch (Exception e) {
            android.util.Log.e("DBUtils", "getCommunityGroupInfo error: " + e.getMessage());
            return null;
        } finally {
            if (conn != null) MySQLHelper.getInstance().releaseConnection(conn);
        }
    }

    /**
     * 推断群主昵称：优先返回 admin_name；为空时取最早加入的成员用户名
     */
    public String getCommunityGroupOwnerName(String groupName) {
        Connection conn = null;
        try {
            conn = MySQLHelper.getInstance().getConnection();
            // 1) 优先 admin_name
            String admin = null;
            try {
                ResultSet rs = conn.createStatement().executeQuery("SELECT admin_name FROM community_groups WHERE name='" + groupName + "'");
                if (rs.next()) admin = rs.getString(1);
                rs.close();
            } catch (Exception ignore) {}
            if (admin != null && !admin.isEmpty()) return admin;
            // 2) 取最早加入成员
            try {
                String sql = "SELECT user_name FROM community_group_members WHERE group_name=? AND joined=1 ORDER BY IFNULL(joined_at, CURRENT_TIMESTAMP) ASC LIMIT 1";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, groupName);
                ResultSet rs = stmt.executeQuery();
                String owner = null;
                if (rs.next()) owner = rs.getString(1);
                rs.close(); stmt.close();
                return owner;
            } catch (Exception ignore) {}
            return null;
        } catch (Exception e) {
            android.util.Log.e("DBUtils", "getCommunityGroupOwnerName error: " + e.getMessage());
            return null;
        } finally {
            if (conn != null) MySQLHelper.getInstance().releaseConnection(conn);
        }
    }

    public boolean setCommunityGroupOwner(String groupName, String userName) {
        Connection conn = null;
        try {
            conn = MySQLHelper.getInstance().getConnection();
            String sql = "UPDATE community_groups SET admin_name=? WHERE name=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, userName);
            stmt.setString(2, groupName);
            int rows = stmt.executeUpdate();
            stmt.close();
            return rows > 0;
        } catch (Exception e) {
            android.util.Log.e("DBUtils", "setCommunityGroupOwner error: " + e.getMessage());
            return false;
        } finally {
            if (conn != null) MySQLHelper.getInstance().releaseConnection(conn);
        }
    }

    /**
     * 获取加入成员的真实数量
     */
    public int getCommunityGroupMemberCount(String groupName) throws SQLException {
        Connection conn = null;
        try {
            conn = MySQLHelper.getInstance().getConnection();
            String sql = "SELECT COUNT(*) FROM community_group_members WHERE group_name=? AND joined=1";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, groupName);
            ResultSet rs = stmt.executeQuery();
            int count = 0;
            if (rs.next()) count = rs.getInt(1);
            rs.close(); stmt.close();
            PreparedStatement sAdmin = conn.prepareStatement("SELECT admin_name FROM community_groups WHERE name=?");
            sAdmin.setString(1, groupName);
            ResultSet rAdmin = sAdmin.executeQuery();
            String admin = null;
            if (rAdmin.next()) admin = rAdmin.getString(1);
            rAdmin.close(); sAdmin.close();
            if (admin != null && !admin.isEmpty()) {
                PreparedStatement upsert = conn.prepareStatement("INSERT INTO community_group_members (group_name, user_name, joined) VALUES (?, ?, 1) ON DUPLICATE KEY UPDATE joined=VALUES(joined), joined_at=CURRENT_TIMESTAMP");
                upsert.setString(1, groupName);
                upsert.setString(2, admin);
                upsert.executeUpdate();
                upsert.close();
                PreparedStatement stmt3 = conn.prepareStatement(sql);
                stmt3.setString(1, groupName);
                ResultSet rs3 = stmt3.executeQuery();
                if (rs3.next()) count = rs3.getInt(1);
                rs3.close(); stmt3.close();
            }
            PreparedStatement sUpdate = conn.prepareStatement("UPDATE community_groups SET member_count=? WHERE name=?");
            sUpdate.setInt(1, count);
            sUpdate.setString(2, groupName);
            sUpdate.executeUpdate();
            sUpdate.close();
            return count;
        } catch (Exception e) {
            throw new SQLException(e.getMessage());
        } finally {
            if (conn != null) MySQLHelper.getInstance().releaseConnection(conn);
        }
    }

    public boolean updateCommunityGroupInfo(String name, String description, String rulesJson, String schedule) {
        Connection conn = null;
        try {
            conn = MySQLHelper.getInstance().getConnection();
            String sql = "UPDATE community_groups SET description=?, rules_json=?, schedule=?, updated_at=CURRENT_TIMESTAMP WHERE name=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, description);
            stmt.setString(2, rulesJson);
            stmt.setString(3, schedule);
            stmt.setString(4, name);
            int rows = stmt.executeUpdate();
            stmt.close();
            return rows > 0;
        } catch (Exception e) {
            android.util.Log.e("DBUtils", "updateCommunityGroupInfo error: " + e.getMessage());
            return false;
        } finally {
            if (conn != null) MySQLHelper.getInstance().releaseConnection(conn);
        }
    }

    public boolean setCommunityGroupJoin(String groupName, String userName, boolean join) {
        Connection conn = null;
        try {
            conn = MySQLHelper.getInstance().getConnection();
            String upsert = "INSERT INTO community_group_members (group_name, user_name, joined) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE joined=VALUES(joined), joined_at=CURRENT_TIMESTAMP";
            PreparedStatement stmt = conn.prepareStatement(upsert);
            stmt.setString(1, groupName);
            stmt.setString(2, userName);
            stmt.setInt(3, join ? 1 : 0);
            int rows = stmt.executeUpdate();
            stmt.close();
            // 更新成员数
            String updateCount = join ?
                "UPDATE community_groups SET member_count = member_count + 1 WHERE name = ?" :
                "UPDATE community_groups SET member_count = GREATEST(member_count - 1, 0) WHERE name = ?";
            PreparedStatement stmt2 = conn.prepareStatement(updateCount);
            stmt2.setString(1, groupName);
            stmt2.executeUpdate();
            stmt2.close();
            return rows > 0;
        } catch (Exception e) {
            android.util.Log.e("DBUtils", "setCommunityGroupJoin error: " + e.getMessage());
            return false;
        } finally {
            if (conn != null) MySQLHelper.getInstance().releaseConnection(conn);
        }
    }

    public List<String> listCommunityGroups() {
        List<String> names = new ArrayList<>();
        Connection conn = null;
        try {
            conn = MySQLHelper.getInstance().getConnection();
            String sql = "SELECT name FROM community_groups ORDER BY updated_at DESC LIMIT 50";
            ResultSet rs = conn.createStatement().executeQuery(sql);
            while (rs.next()) names.add(rs.getString(1));
            rs.close();
        } catch (Exception e) {
            android.util.Log.e("DBUtils", "listCommunityGroups error: " + e.getMessage());
        } finally {
            if (conn != null) MySQLHelper.getInstance().releaseConnection(conn);
        }
        return names;
    }

    public List<String> getSharedGroups(String userName) {
        List<String> names = new ArrayList<>();
        Connection conn = null;
        try {
            conn = MySQLHelper.getInstance().getConnection();
            String sql = "SELECT name FROM community_groups WHERE admin_name=? UNION SELECT group_name AS name FROM community_group_members WHERE user_name=? AND joined=1";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, userName);
            stmt.setString(2, userName);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) names.add(rs.getString(1));
            rs.close(); stmt.close();
        } catch (Exception e) {
            android.util.Log.e("DBUtils", "getSharedGroups error: " + e.getMessage());
        } finally {
            if (conn != null) MySQLHelper.getInstance().releaseConnection(conn);
        }
        return names;
    }

    // 获取视频播放历史（异步方法）
    public void getVideoPlayHistory(String userName, final VideoHistoryCallback callback) {
        List<VideoBean> historyList = new ArrayList<>();
        
        if (helper == null) {
            android.util.Log.e("DBUtils", "数据库连接未初始化");
            android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            mainHandler.post(() -> callback.onResult(historyList));
            return;
        }
        
        new Thread(() -> {
            helper.getConnection(new MySQLHelper.ConnectionResultCallback() {
                @Override
                public void onSuccess(Connection conn) {
                    try {
                        // 对同一视频去重，取最新一条，并按播放时间排序
                        String sql = "SELECT vp.* FROM videoplaylist vp " +
                                "INNER JOIN (SELECT chapterId, videoId, MAX(_id) AS max_id FROM videoplaylist WHERE userName = ? GROUP BY chapterId, videoId) t " +
                                "ON vp.chapterId = t.chapterId AND vp.videoId = t.videoId AND vp._id = t.max_id " +
                                "WHERE vp.userName = ? " +
                                "ORDER BY IFNULL(vp.playTimestamp, 0) DESC, vp._id DESC";
                        PreparedStatement stmt = conn.prepareStatement(sql);
                        stmt.setString(1, userName);
                        stmt.setString(2, userName);
                        ResultSet rs = stmt.executeQuery();
                        
                        while (rs.next()) {
                            VideoBean videoBean = new VideoBean();
                            videoBean.chapterId = rs.getInt("chapterId");
                            videoBean.videoId = rs.getInt("videoId");
                            videoBean.title = rs.getString("title");
                            videoBean.secondTitle = rs.getString("secondTitle");
                            videoBean.videoPath = rs.getString("videoPath");
                            // 优先使用真实播放时间戳，兼容无该列的旧结构
                            try {
                                long ts = rs.getLong("playTimestamp");
                                videoBean.playTime = ts;
                            } catch (SQLException ignoreColumn) {
                                videoBean.playTime = 0L;
                            }
                            historyList.add(videoBean);
                        }
                        
                        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                        mainHandler.post(() -> callback.onResult(historyList));
                    } catch (SQLException e) {
                        e.printStackTrace();
                        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                        mainHandler.post(() -> callback.onResult(historyList));
                    } finally {
                        helper.releaseConnection(conn);
                    }
                }
                
                @Override
                public void onError(SQLException e) {
                    android.util.Log.e("DBUtils", "获取连接失败: " + e.getMessage());
                    android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                    mainHandler.post(() -> callback.onResult(new ArrayList<>()));
                }
            });
        }).start();
    }
    
    public interface VideoHistoryCallback {
        void onResult(List<VideoBean> historyList);
    }

    // 清空用户的所有播放历史
    public interface ClearHistoryCallback {
        void onResult(boolean success);
    }
    
    public void clearVideoPlayHistory(String userName, final ClearHistoryCallback callback) {
        if (helper == null) {
            android.util.Log.e("DBUtils", "数据库连接未初始化");
            android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            mainHandler.post(() -> callback.onResult(false));
            return;
        }
        
        new Thread(() -> {
            helper.getConnection(new MySQLHelper.ConnectionResultCallback() {
                @Override
                public void onSuccess(Connection conn) {
                    try {
                        String sql = "DELETE FROM videoplaylist WHERE userName = ?";
                        PreparedStatement stmt = conn.prepareStatement(sql);
                        stmt.setString(1, userName);
                        boolean success = stmt.executeUpdate() >= 0;
                        
                        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                        mainHandler.post(() -> callback.onResult(success));
                    } catch (SQLException e) {
                        e.printStackTrace();
                        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                        mainHandler.post(() -> callback.onResult(false));
                    } finally {
                        helper.releaseConnection(conn);
                    }
                }
                
                @Override
                public void onError(SQLException e) {
                    android.util.Log.e("DBUtils", "获取连接失败: " + e.getMessage());
                    android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                    mainHandler.post(() -> callback.onResult(false));
                }
            });
        }).start();
    }
    
    /**
     * @deprecated 使用 {@link #clearVideoPlayHistory(String, ClearHistoryCallback)} 代替
     */
    @Deprecated
    public boolean clearVideoPlayHistory(String userName) {
        if (helper == null) {
            android.util.Log.e("DBUtils", "数据库连接未初始化");
            return false;
        }
        
        Connection conn = null;
        try {
            conn = helper.getConnection();
            if (conn == null) {
                android.util.Log.e("DBUtils", "无法获取数据库连接");
                return false;
            }
            
            String sql = "DELETE FROM videoplaylist WHERE userName = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, userName);
            return stmt.executeUpdate() >= 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                helper.releaseConnection(conn);
            }
        }
    }

    /**
     * 通过手机号重置密码（假设手机号即username）
     */
    public boolean updateUserPasswordByPhone(String phone, String newPassword) {
        Connection conn = null;
        try {
            conn = helper.getConnection();
            String sql = "UPDATE user_info SET password=?, updated_at=NOW() WHERE username=?;";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, newPassword);
            stmt.setString(2, phone);
            int rows = stmt.executeUpdate();
            stmt.close();
            return rows > 0;
        } catch (SQLException e) {
            android.util.Log.e("DBUtils", "重置密码失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                helper.releaseConnection(conn);
            }
        }
    }
}
