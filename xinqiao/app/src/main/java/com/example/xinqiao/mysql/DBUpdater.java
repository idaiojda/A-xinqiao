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
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
                        if (shouldSkip(conn, sql)) {
                            Log.d(TAG, "Skipped SQL (already applied): " + sql);
                        } else {
                            stmt.execute(sql);
                            Log.d(TAG, "Executed SQL: " + sql);
                        }
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

    private boolean shouldSkip(Connection conn, String sql) {
        String s = sql.toLowerCase();
        if (s.startsWith("alter table") && s.contains("add column")) {
            String[] parts = sql.replaceAll("\\s+", " ").split(" ");
            // naive parse: ALTER TABLE <table> ADD COLUMN <column> ...
            try {
                int idxTable = 2; // ALTER TABLE <table>
                String table = parts[idxTable].replace("`", "");
                int idxAddCol = 5; // ADD COLUMN <column>
                String column = parts[idxAddCol].replace("`", "");
                return columnExists(conn, table, column);
            } catch (Exception ignored) { return false; }
        }
        if (s.startsWith("create index") && s.contains(" on ")) {
            // CREATE INDEX <index> ON <table>(...)
            try {
                String norm = sql.replaceAll("\\s+", " ");
                Matcher m = Pattern.compile("(?i)create index\\s+`?([a-zA-Z0-9_]+)`?\\s+on\\s+`?([a-zA-Z0-9_]+)`?").matcher(norm);
                if (m.find()) {
                    String index = m.group(1);
                    String table = m.group(2);
                    return indexExists(conn, table, index);
                }
            } catch (Exception ignored) { }
        }
        return false;
    }

    private boolean columnExists(Connection conn, String table, String column) {
        String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, table);
            ps.setString(2, column);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException ignored) {}
        return false;
    }

    private boolean indexExists(Connection conn, String table, String index) {
        String sql = "SHOW INDEX FROM `" + table + "` WHERE Key_name = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, index);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ignored) {}
        return false;
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