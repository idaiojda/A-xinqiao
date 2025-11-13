package com.example.xinqiao.util;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.CancellationSignal;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据库优化工具类
 * 提供一系列方法来优化数据库操作
 */
public class DatabaseOptimizer {

    /**
     * 批量插入数据
     * @param db 数据库实例
     * @param tableName 表名
     * @param valuesList 值列表
     * @return 插入的行数
     */
    public static int batchInsert(SQLiteDatabase db, String tableName, List<ContentValues> valuesList) {
        if (db == null || tableName == null || valuesList == null || valuesList.isEmpty()) {
            return 0;
        }
        
        int insertCount = 0;
        
        // 开始事务
        db.beginTransaction();
        try {
            for (ContentValues values : valuesList) {
                long id = db.insert(tableName, null, values);
                if (id != -1) {
                    insertCount++;
                }
            }
            // 标记事务成功
            db.setTransactionSuccessful();
        } finally {
            // 结束事务
            db.endTransaction();
        }
        
        return insertCount;
    }
    
    /**
     * 使用SQL语句批量插入数据（更高效）
     * @param db 数据库实例
     * @param sql 插入SQL语句（使用?占位符）
     * @param bindArgs 绑定参数列表
     * @return 是否成功
     */
    public static boolean batchInsertWithSql(SQLiteDatabase db, String sql, List<Object[]> bindArgs) {
        if (db == null || sql == null || bindArgs == null || bindArgs.isEmpty()) {
            return false;
        }
        
        // 开始事务
        db.beginTransaction();
        try {
            SQLiteStatement statement = db.compileStatement(sql);
            for (Object[] args : bindArgs) {
                statement.clearBindings();
                for (int i = 0; i < args.length; i++) {
                    bindValue(statement, i + 1, args[i]);
                }
                statement.execute();
            }
            // 标记事务成功
            db.setTransactionSuccessful();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            // 结束事务
            db.endTransaction();
        }
    }
    
    /**
     * 绑定值到SQLiteStatement
     * @param statement SQLiteStatement实例
     * @param index 索引（从1开始）
     * @param value 值
     */
    private static void bindValue(SQLiteStatement statement, int index, Object value) {
        if (value == null) {
            statement.bindNull(index);
        } else if (value instanceof String) {
            statement.bindString(index, (String) value);
        } else if (value instanceof Long) {
            statement.bindLong(index, (Long) value);
        } else if (value instanceof Integer) {
            statement.bindLong(index, (Integer) value);
        } else if (value instanceof Double) {
            statement.bindDouble(index, (Double) value);
        } else if (value instanceof Float) {
            statement.bindDouble(index, (Float) value);
        } else if (value instanceof byte[]) {
            statement.bindBlob(index, (byte[]) value);
        } else if (value instanceof Boolean) {
            statement.bindLong(index, ((Boolean) value) ? 1 : 0);
        } else {
            statement.bindString(index, value.toString());
        }
    }
    
    /**
     * 异步查询数据库
     * @param db 数据库实例
     * @param table 表名
     * @param columns 列名数组
     * @param selection 选择条件
     * @param selectionArgs 选择参数
     * @param groupBy 分组条件
     * @param having 分组筛选
     * @param orderBy 排序条件
     * @param limit 限制条数
     * @param callback 查询回调
     */
    public static void queryAsync(SQLiteDatabase db, String table, String[] columns,
                                 String selection, String[] selectionArgs, String groupBy,
                                 String having, String orderBy, String limit, QueryCallback callback) {
        if (db == null || table == null || callback == null) {
            if (callback != null) {
                callback.onQueryComplete(null);
            }
            return;
        }
        
        // 在后台线程执行查询
        ThreadOptimizer.getInstance().executeDiskIO(() -> {
            Cursor cursor = null;
            try {
                cursor = db.query(table, columns, selection, selectionArgs, groupBy, having, orderBy, limit);
                final Cursor finalCursor = cursor;
                // 在主线程返回结果
                ThreadOptimizer.getInstance().executeMainThread(() -> callback.onQueryComplete(finalCursor));
                cursor = null; // 防止finally中关闭cursor
            } catch (Exception e) {
                e.printStackTrace();
                ThreadOptimizer.getInstance().executeMainThread(() -> callback.onQueryError(e));
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        });
    }
    
    /**
     * 带取消功能的异步查询
     * @param db 数据库实例
     * @param sql SQL语句
     * @param selectionArgs 选择参数
     * @param cancellationSignal 取消信号
     * @param callback 查询回调
     */
    public static void rawQueryAsync(SQLiteDatabase db, String sql, String[] selectionArgs,
                                    CancellationSignal cancellationSignal, QueryCallback callback) {
        if (db == null || sql == null || callback == null) {
            if (callback != null) {
                callback.onQueryComplete(null);
            }
            return;
        }
        
        // 在后台线程执行查询
        ThreadOptimizer.getInstance().executeDiskIO(() -> {
            Cursor cursor = null;
            try {
                cursor = db.rawQuery(sql, selectionArgs, cancellationSignal);
                final Cursor finalCursor = cursor;
                // 在主线程返回结果
                ThreadOptimizer.getInstance().executeMainThread(() -> callback.onQueryComplete(finalCursor));
                cursor = null; // 防止finally中关闭cursor
            } catch (Exception e) {
                e.printStackTrace();
                ThreadOptimizer.getInstance().executeMainThread(() -> callback.onQueryError(e));
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        });
    }
    
    /**
     * 查询回调接口
     */
    public interface QueryCallback {
        void onQueryComplete(Cursor cursor);
        void onQueryError(Exception e);
    }
}