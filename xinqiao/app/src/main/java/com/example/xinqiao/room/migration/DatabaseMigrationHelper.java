package com.example.xinqiao.room.migration;

import android.content.Context;
import android.util.Log;

import com.example.xinqiao.mysql.MySQLHelper;
import com.example.xinqiao.room.AppDatabase;
import com.example.xinqiao.room.entity.UserInfo;
import com.example.xinqiao.room.repository.UserRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 数据库迁移助手类，用于将MySQL数据迁移到Room数据库
 */
public class DatabaseMigrationHelper {
    
    private static final String TAG = "DatabaseMigration";
    
    /**
     * 迁移用户数据
     * @param context 上下文
     * @param callback 迁移回调
     */
    public static void migrateUserData(Context context, final MigrationCallback callback) {
        // 获取MySQL实例
        MySQLHelper mySQLHelper;
        try {
            mySQLHelper = MySQLHelper.getInstance();
        } catch (IllegalStateException e) {
            Log.e(TAG, "MySQL未初始化: " + e.getMessage());
            if (callback != null) {
                callback.onError(e);
            }
            return;
        }
        
        // 创建用户仓库
        UserRepository userRepository = new UserRepository(context);
        
        // 在新线程中执行迁移操作
        new Thread(() -> {
            Connection conn = null;
            try {
                // 获取MySQL连接
                conn = mySQLHelper.getConnection();
                if (conn == null) {
                    throw new SQLException("无法获取MySQL连接");
                }
                
                // 查询所有用户数据
                String sql = "SELECT * FROM user_info";
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery();
                
                // 记录迁移状态
                final AtomicBoolean migrationSuccess = new AtomicBoolean(true);
                int totalCount = 0;
                int successCount = 0;
                
                // 遍历结果集，将数据插入Room数据库
                while (rs.next()) {
                    totalCount++;
                    
                    // 创建UserInfo对象
                    UserInfo userInfo = new UserInfo();
                    userInfo.setUserId(rs.getInt("user_id"));
                    userInfo.setUsername(rs.getString("username"));
                    userInfo.setPassword(rs.getString("password"));
                    userInfo.setNickname(rs.getString("nickname"));
                    userInfo.setGender(rs.getString("gender"));
                    userInfo.setBirthday(rs.getDate("birthday"));
                    userInfo.setMaritalStatus(rs.getString("marital_status"));
                    userInfo.setOccupation(rs.getString("occupation"));
                    userInfo.setIntroduction(rs.getString("introduction"));
                    userInfo.setAvatar(rs.getBytes("avatar"));
                    userInfo.setBalance(rs.getDouble("balance"));
                    userInfo.setCreatedAt(rs.getTimestamp("created_at"));
                    userInfo.setUpdatedAt(rs.getTimestamp("updated_at"));
                    
                    // 使用CountDownLatch等待插入操作完成
                    final CountDownLatch latch = new CountDownLatch(1);
                    final int currentIndex = totalCount;
                    
                    // 插入用户数据到Room数据库
                    userRepository.insert(userInfo, new UserRepository.OperationCallback<Long>() {
                        @Override
                        public void onSuccess(Long result) {
                            Log.d(TAG, String.format("用户数据迁移成功 (%d): %s", currentIndex, userInfo.getUsername()));
                            latch.countDown();
                        }
                        
                        @Override
                        public void onError(Exception e) {
                            Log.e(TAG, String.format("用户数据迁移失败 (%d): %s - %s", currentIndex, userInfo.getUsername(), e.getMessage()));
                            migrationSuccess.set(false);
                            latch.countDown();
                        }
                    });
                    
                    // 等待插入操作完成
                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        Log.e(TAG, "迁移过程被中断: " + e.getMessage());
                        migrationSuccess.set(false);
                    }
                    
                    // 如果插入成功，增加成功计数
                    if (migrationSuccess.get()) {
                        successCount++;
                    }
                }
                
                // 关闭结果集和语句
                rs.close();
                stmt.close();
                
                // 迁移完成回调
                final int finalTotalCount = totalCount;
                final int finalSuccessCount = successCount;
                if (callback != null) {
                    if (migrationSuccess.get()) {
                        callback.onSuccess(finalTotalCount, finalSuccessCount);
                    } else {
                        callback.onPartialSuccess(finalTotalCount, finalSuccessCount);
                    }
                }
                
                Log.i(TAG, String.format("用户数据迁移完成: 总计 %d 条记录, 成功 %d 条", finalTotalCount, finalSuccessCount));
                
            } catch (SQLException e) {
                Log.e(TAG, "数据迁移过程中发生SQL异常: " + e.getMessage());
                if (callback != null) {
                    callback.onError(e);
                }
            } finally {
                // 释放MySQL连接
                if (conn != null) {
                    mySQLHelper.releaseConnection(conn);
                }
            }
        }).start();
    }
    
    /**
     * 迁移回调接口
     */
    public interface MigrationCallback {
        /**
         * 迁移成功回调
         * @param totalCount 总记录数
         * @param successCount 成功迁移的记录数
         */
        void onSuccess(int totalCount, int successCount);
        
        /**
         * 部分迁移成功回调
         * @param totalCount 总记录数
         * @param successCount 成功迁移的记录数
         */
        void onPartialSuccess(int totalCount, int successCount);
        
        /**
         * 迁移失败回调
         * @param e 异常信息
         */
        void onError(Exception e);
    }
}