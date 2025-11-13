package com.example.xinqiao.utils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * 数据库帮助类，用于创建和管理应用的SQLite数据库
 */
public class DBHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "xinqiao.db";
    private static final int DATABASE_VERSION = 1;
    private static DBHelper instance;

    /**
     * 获取DBHelper实例（单例模式）
     */
    public static synchronized DBHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DBHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 创建文章收藏表
        String createFavoriteTableSql = "CREATE TABLE IF NOT EXISTS article_favorite (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_name TEXT, " +
                "article_id INTEGER, " +
                "title TEXT, " +
                "content TEXT, " +
                "category TEXT, " +
                "summary TEXT, " +
                "image_res_id INTEGER, " +
                "favorite_timestamp INTEGER, " +
                "UNIQUE(user_name, article_id)" +
                ")";
        db.execSQL(createFavoriteTableSql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 数据库版本升级时的处理
        if (oldVersion < newVersion) {
            // 可以在这里添加数据库升级逻辑
            // 例如：添加新表、修改表结构等
        }
    }
}