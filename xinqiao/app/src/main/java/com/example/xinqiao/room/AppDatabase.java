package com.example.xinqiao.room;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.xinqiao.room.converter.DateConverter;
import com.example.xinqiao.room.dao.UserInfoDao;
import com.example.xinqiao.room.entity.UserInfo;

/**
 * 应用数据库类，使用Room框架管理SQLite数据库
 */
@Database(
    entities = {UserInfo.class},
    version = 1,
    exportSchema = false
)
@TypeConverters({DateConverter.class})
public abstract class AppDatabase extends RoomDatabase {
    
    private static final String DATABASE_NAME = "xinqiao.db";
    private static volatile AppDatabase INSTANCE;
    
    /**
     * 获取UserInfoDao实例
     * @return UserInfoDao实例
     */
    public abstract UserInfoDao userInfoDao();
    
    /**
     * 获取数据库实例（单例模式）
     * @param context 上下文
     * @return 数据库实例
     */
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, DATABASE_NAME)
                            .addCallback(new Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    // 数据库创建时的回调，可以在这里初始化数据
                                    android.util.Log.i("AppDatabase", "数据库创建成功");
                                }
                                
                                @Override
                                public void onOpen(@NonNull SupportSQLiteDatabase db) {
                                    super.onOpen(db);
                                    // 数据库打开时的回调
                                    android.util.Log.i("AppDatabase", "数据库打开成功");
                                }
                            })
                            // 允许在主线程中进行查询（不推荐在生产环境中使用）
                            // .allowMainThreadQueries()
                            // 设置迁移策略，如果没有找到迁移路径，则重新创建数据库（仅在开发阶段使用）
                            // .fallbackToDestructiveMigration()
                            // 添加迁移策略
                            // .addMigrations(MIGRATION_1_2)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
    
    /**
     * 从版本1迁移到版本2的迁移策略示例
     */
    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // 在这里编写迁移代码，例如：
            // database.execSQL("ALTER TABLE user_info ADD COLUMN new_column TEXT");
        }
    };
    
    /**
     * 关闭数据库实例
     */
    public static void closeInstance() {
        if (INSTANCE != null && INSTANCE.isOpen()) {
            INSTANCE.close();
            INSTANCE = null;
        }
    }
}