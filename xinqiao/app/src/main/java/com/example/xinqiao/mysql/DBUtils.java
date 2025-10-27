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
import com.example.xinqiao.utils.AnalysisUtils;
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
                        String sql = "INSERT INTO videoplaylist (userName, chapterId, videoId, videoPath, title, secondTitle) VALUES (?, ?, ?, ?, ?, ?);";
                        PreparedStatement stmt = conn.prepareStatement(sql);
                        stmt.setString(1, userName);
                        stmt.setInt(2, chapterId);
                        stmt.setInt(3, videoId);
                        stmt.setString(4, videoPath);
                        stmt.setString(5, title);
                        stmt.setString(6, secondTitle);
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
            String sql = "INSERT INTO videoplaylist (userName, chapterId, videoId, videoPath, title, secondTitle) VALUES (?, ?, ?, ?, ?, ?);";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, userName);
            stmt.setInt(2, chapterId);
            stmt.setInt(3, videoId);
            stmt.setString(4, videoPath);
            stmt.setString(5, title);
            stmt.setString(6, secondTitle);
            return stmt.executeUpdate() > 0;
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
                        String sql = "SELECT * FROM videoplaylist WHERE userName = ? ORDER BY _id DESC";
                        PreparedStatement stmt = conn.prepareStatement(sql);
                        stmt.setString(1, userName);
                        ResultSet rs = stmt.executeQuery();
                        
                        while (rs.next()) {
                            VideoBean videoBean = new VideoBean();
                            videoBean.chapterId = rs.getInt("chapterId");
                            videoBean.videoId = rs.getInt("videoId");
                            videoBean.title = rs.getString("title");
                            videoBean.secondTitle = rs.getString("secondTitle");
                            videoBean.videoPath = rs.getString("videoPath");
                            // 使用当前时间作为播放时间（实际项目中应该存储真实的播放时间）
                            videoBean.playTime = System.currentTimeMillis();
                            historyList.add(videoBean);
                        }
                        
                        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                        mainHandler.post(() -> callback.onResult(historyList));
                    } catch (SQLException e) {
                        e.printStackTrace();
                        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
                        mainHandler.post(() -> callback.onResult(new ArrayList<>()));
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