package com.example.xinqiao.room.example;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;

import com.example.xinqiao.room.RoomDatabaseManager;
import com.example.xinqiao.room.entity.UserInfo;
import com.example.xinqiao.room.repository.UserRepository;

import java.util.Date;
import java.util.List;

/**
 * Room ORM框架使用示例
 * 本类展示如何在Activity中使用Room ORM框架替代原有的MySQL操作
 */
public class RoomUsageExample extends AppCompatActivity {
    
    private static final String TAG = "RoomUsageExample";
    private RoomDatabaseManager dbManager;
    private UserRepository userRepository;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 初始化数据库管理器
        dbManager = RoomDatabaseManager.getInstance(this);
        dbManager.initialize(new RoomDatabaseManager.InitCallback() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "Room数据库初始化成功");
                Toast.makeText(RoomUsageExample.this, "数据库初始化成功", Toast.LENGTH_SHORT).show();
                
                // 获取用户仓库
                userRepository = dbManager.getUserRepository();
                
                // 示例：执行数据库操作
                performDatabaseOperations();
            }
            
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Room数据库初始化失败: " + e.getMessage());
                Toast.makeText(RoomUsageExample.this, "数据库初始化失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * 执行数据库操作示例
     */
    private void performDatabaseOperations() {
        // 示例1：插入用户
        insertUserExample();
        
        // 示例2：查询用户
        queryUserExample();
        
        // 示例3：更新用户
        updateUserExample();
        
        // 示例4：使用LiveData观察数据变化
        observeUsersExample();
    }
    
    /**
     * 插入用户示例
     */
    private void insertUserExample() {
        // 创建用户对象
        UserInfo user = new UserInfo();
        user.setUsername("testuser");
        user.setPassword("123456");
        user.setNickname("测试用户");
        user.setGender("男");
        user.setBalance(0.00);
        user.setCreatedAt(new Date());
        user.setUpdatedAt(new Date());
        
        // 插入用户
        userRepository.insert(user, new UserRepository.OperationCallback<Long>() {
            @Override
            public void onSuccess(Long result) {
                Log.i(TAG, "用户插入成功，ID: " + result);
                Toast.makeText(RoomUsageExample.this, "用户插入成功，ID: " + result, Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "用户插入失败: " + e.getMessage());
                Toast.makeText(RoomUsageExample.this, "用户插入失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * 查询用户示例
     */
    private void queryUserExample() {
        // 根据用户名查询用户
        userRepository.getUserByUsername("testuser", new UserRepository.OperationCallback<UserInfo>() {
            @Override
            public void onSuccess(UserInfo result) {
                if (result != null) {
                    Log.i(TAG, "查询用户成功: " + result.getUsername() + ", 昵称: " + result.getNickname());
                    Toast.makeText(RoomUsageExample.this, "查询用户成功: " + result.getNickname(), Toast.LENGTH_SHORT).show();
                } else {
                    Log.i(TAG, "未找到用户");
                    Toast.makeText(RoomUsageExample.this, "未找到用户", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "查询用户失败: " + e.getMessage());
                Toast.makeText(RoomUsageExample.this, "查询用户失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * 更新用户示例
     */
    private void updateUserExample() {
        // 先查询用户
        userRepository.getUserByUsername("testuser", new UserRepository.OperationCallback<UserInfo>() {
            @Override
            public void onSuccess(UserInfo result) {
                if (result != null) {
                    // 更新用户信息
                    result.setNickname("更新后的昵称");
                    result.setGender("女");
                    result.setUpdatedAt(new Date());
                    
                    // 保存更新
                    userRepository.update(result, new UserRepository.OperationCallback<Integer>() {
                        @Override
                        public void onSuccess(Integer result) {
                            Log.i(TAG, "用户更新成功，影响行数: " + result);
                            Toast.makeText(RoomUsageExample.this, "用户更新成功", Toast.LENGTH_SHORT).show();
                        }
                        
                        @Override
                        public void onError(Exception e) {
                            Log.e(TAG, "用户更新失败: " + e.getMessage());
                            Toast.makeText(RoomUsageExample.this, "用户更新失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Log.i(TAG, "未找到要更新的用户");
                    Toast.makeText(RoomUsageExample.this, "未找到要更新的用户", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "查询用户失败: " + e.getMessage());
                Toast.makeText(RoomUsageExample.this, "查询用户失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * 使用LiveData观察数据变化示例
     */
    private void observeUsersExample() {
        // 获取所有用户的LiveData
        userRepository.getAllUsersLive().observe(this, new Observer<List<UserInfo>>() {
            @Override
            public void onChanged(List<UserInfo> users) {
                // 数据变化时会自动调用此方法
                Log.i(TAG, "用户数据变化，当前用户数: " + users.size());
                
                // 这里可以更新UI，例如刷新RecyclerView等
                StringBuilder sb = new StringBuilder("用户列表:\n");
                for (UserInfo user : users) {
                    sb.append(user.getUsername()).append(" - ").append(user.getNickname()).append("\n");
                }
                
                Log.d(TAG, sb.toString());
            }
        });
    }
    
    /**
     * 从MySQL迁移数据到Room示例
     */
    private void migrateFromMySQLExample() {
        dbManager.migrateFromMySQL(new RoomDatabaseManager.MigrationCallback() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "数据迁移成功");
                Toast.makeText(RoomUsageExample.this, "数据迁移成功", Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onPartialSuccess(String message) {
                Log.w(TAG, "数据迁移部分成功: " + message);
                Toast.makeText(RoomUsageExample.this, "数据迁移部分成功: " + message, Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "数据迁移失败: " + e.getMessage());
                Toast.makeText(RoomUsageExample.this, "数据迁移失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 关闭数据库连接
        if (dbManager != null) {
            dbManager.closeDatabase();
        }
    }
}