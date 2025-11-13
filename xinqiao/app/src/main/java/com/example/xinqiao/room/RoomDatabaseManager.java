package com.example.xinqiao.room;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.example.xinqiao.room.entity.UserInfo;
import com.example.xinqiao.room.migration.DatabaseMigrationHelper;
import com.example.xinqiao.room.repository.UserRepository;

import java.util.List;

/**
 * Room数据库管理器，提供统一的数据库访问接口
 */
public class RoomDatabaseManager {
    
    private static final String TAG = "RoomDatabaseManager";
    private static RoomDatabaseManager instance;
    
    private final Context context;
    private final UserRepository userRepository;
    private boolean initialized = false;
    
    /**
     * 私有构造函数
     * @param context 上下文
     */
    private RoomDatabaseManager(Context context) {
        this.context = context.getApplicationContext();
        this.userRepository = new UserRepository(this.context);
    }
    
    /**
     * 获取实例（单例模式）
     * @param context 上下文
     * @return RoomDatabaseManager实例
     */
    public static synchronized RoomDatabaseManager getInstance(Context context) {
        if (instance == null) {
            instance = new RoomDatabaseManager(context);
        }
        return instance;
    }
    
    /**
     * 初始化数据库
     * @param callback 初始化回调
     */
    public void initialize(final InitCallback callback) {
        if (initialized) {
            if (callback != null) {
                callback.onSuccess();
            }
            return;
        }
        
        try {
            // 获取数据库实例，触发数据库创建或打开
            AppDatabase.getInstance(context);
            initialized = true;
            
            if (callback != null) {
                callback.onSuccess();
            }
            
            Log.i(TAG, "Room数据库初始化成功");
        } catch (Exception e) {
            Log.e(TAG, "Room数据库初始化失败: " + e.getMessage());
            if (callback != null) {
                callback.onError(e);
            }
        }
    }
    
    /**
     * 从MySQL迁移数据到Room数据库
     * @param callback 迁移回调
     */
    public void migrateFromMySQL(final MigrationCallback callback) {
        if (!initialized) {
            if (callback != null) {
                callback.onError(new IllegalStateException("数据库未初始化"));
            }
            return;
        }
        
        DatabaseMigrationHelper.migrateUserData(context, new DatabaseMigrationHelper.MigrationCallback() {
            @Override
            public void onSuccess(int totalCount, int successCount) {
                Log.i(TAG, String.format("数据迁移成功: 总计 %d 条记录, 成功 %d 条", totalCount, successCount));
                if (callback != null) {
                    callback.onSuccess();
                }
            }
            
            @Override
            public void onPartialSuccess(int totalCount, int successCount) {
                Log.w(TAG, String.format("数据迁移部分成功: 总计 %d 条记录, 成功 %d 条", totalCount, successCount));
                if (callback != null) {
                    callback.onPartialSuccess(String.format("总计 %d 条记录, 成功 %d 条", totalCount, successCount));
                }
            }
            
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "数据迁移失败: " + e.getMessage());
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    /**
     * 获取用户仓库
     * @return 用户仓库
     */
    public UserRepository getUserRepository() {
        return userRepository;
    }
    
    /**
     * 关闭数据库
     */
    public void closeDatabase() {
        AppDatabase.closeInstance();
        initialized = false;
    }
    
    /**
     * 初始化回调接口
     */
    public interface InitCallback {
        /**
         * 初始化成功回调
         */
        void onSuccess();
        
        /**
         * 初始化失败回调
         * @param e 异常信息
         */
        void onError(Exception e);
    }
    
    /**
     * 迁移回调接口
     */
    public interface MigrationCallback {
        /**
         * 迁移成功回调
         */
        void onSuccess();
        
        /**
         * 部分迁移成功回调
         * @param message 部分成功信息
         */
        void onPartialSuccess(String message);
        
        /**
         * 迁移失败回调
         * @param e 异常信息
         */
        void onError(Exception e);
    }
}