package com.example.xinqiao.mysql;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import com.example.xinqiao.mysql.MySQLHelper;

/**
 * 数据库更新工具类
 */
public class DBUpdater {
    private static final String TAG = "DBUpdater";
    private static final String PREF_NAME = "db_version";
    private static final String KEY_VERSION = "version";
    private static final int CURRENT_VERSION = 2; // 当前数据库版本

    private Context context;
    private MySQLHelper dbHelper;

    public DBUpdater(Context context) {
        this.context = context;
        this.dbHelper = MySQLHelper.getInstance();
    }

    /**
     * 检查并更新数据库
     */
    public void checkAndUpdateDatabase() {
        int savedVersion = getSavedVersion();
        if (savedVersion < CURRENT_VERSION) {
            updateDatabase(savedVersion, CURRENT_VERSION);
            saveVersion(CURRENT_VERSION);
        }
    }

    /**
     * 获取保存的数据库版本
     */
    private int getSavedVersion() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_VERSION, 1); // 默认版本为1
    }

    /**
     * 保存数据库版本
     */
    private void saveVersion(int version) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_VERSION, version);
        editor.apply();
    }

    /**
     * 更新数据库
     */
    private void updateDatabase(int fromVersion, int toVersion) {
        Log.d(TAG, "Updating database from version " + fromVersion + " to " + toVersion);
        
        // 读取更新脚本
        String sqlScript = readUpdateScript();
        if (sqlScript == null || sqlScript.isEmpty()) {
            Log.e(TAG, "Failed to read update script");
            return;
        }
        
        // 执行SQL脚本
        Connection conn = null;
        try {
            // 使用同步方式获取连接
            conn = MySQLHelper.getInstance().getConnection();
            if (conn == null) {
                Log.e(TAG, "Failed to get database connection");
                return;
            }
            
            // 分割SQL语句并执行
            String[] sqlStatements = sqlScript.split(";");
            Statement stmt = conn.createStatement();
            
            for (String sql : sqlStatements) {
                sql = sql.trim();
                if (!sql.isEmpty()) {
                    try {
                        stmt.execute(sql);
                        Log.d(TAG, "Executed SQL: " + sql);
                    } catch (SQLException e) {
                        Log.e(TAG, "Error executing SQL: " + sql, e);
                        // 继续执行其他语句
                    }
                }
            }
            
            stmt.close();
            Log.d(TAG, "Database updated successfully");
            
        } catch (SQLException e) {
            Log.e(TAG, "Error updating database", e);
        } finally {
            if (conn != null) {
                MySQLHelper.getInstance().releaseConnection(conn);
            }
        }
    }

    /**
     * 读取更新脚本
     */
    private String readUpdateScript() {
        StringBuilder sb = new StringBuilder();
        try {
            InputStream is = context.getAssets().open("db_update.sql");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                // 忽略注释行
                if (!line.trim().startsWith("--")) {
                    sb.append(line).append("\n");
                }
            }
            reader.close();
            is.close();
        } catch (IOException e) {
            Log.e(TAG, "Error reading update script", e);
            return null;
        }
        return sb.toString();
    }
}