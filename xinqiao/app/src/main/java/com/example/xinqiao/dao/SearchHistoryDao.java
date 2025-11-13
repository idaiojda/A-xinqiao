package com.example.xinqiao.dao;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.xinqiao.mysql.MySQLHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SearchHistoryDao {
    private final Context context;
    private final MySQLHelper helper;
    private final Handler mainHandler;

    public SearchHistoryDao(Context context) {
        this.context = context;
        this.helper = MySQLHelper.getInstance();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public interface HistoryCallback {
        void onSuccess(List<String> history);
        void onError(Exception e);
    }

    public void saveKeywordAsync(String userName, String keyword) {
        new Thread(() -> {
            helper.getConnection(new MySQLHelper.ConnectionResultCallback() {
                @Override
                public void onSuccess(Connection conn) {
                    try {
                        String sql = "INSERT INTO search_history (userName, keyword, createTime) VALUES (?, ?, ?)";
                        PreparedStatement pstmt = conn.prepareStatement(sql);
                        pstmt.setString(1, userName);
                        pstmt.setString(2, keyword);
                        pstmt.setLong(3, System.currentTimeMillis());
                        pstmt.executeUpdate();
                        pstmt.close();
                    } catch (SQLException e) {
                        Log.e("SearchHistoryDao", "保存搜索历史失败: " + e.getMessage());
                    } finally {
                        helper.releaseConnection(conn);
                    }
                }

                @Override
                public void onError(SQLException e) {
                    Log.e("SearchHistoryDao", "获取连接失败: " + e.getMessage());
                }
            });
        }).start();
    }

    public void getHistoryAsync(String userName, int limit, HistoryCallback callback) {
        new Thread(() -> {
            helper.getConnection(new MySQLHelper.ConnectionResultCallback() {
                @Override
                public void onSuccess(Connection conn) {
                    List<String> list = new ArrayList<>();
                    try {
                        String sql = "SELECT keyword FROM search_history WHERE userName=? ORDER BY createTime DESC LIMIT ?";
                        PreparedStatement pstmt = conn.prepareStatement(sql);
                        pstmt.setString(1, userName);
                        pstmt.setInt(2, limit);
                        ResultSet rs = pstmt.executeQuery();
                        while (rs.next()) {
                            list.add(rs.getString("keyword"));
                        }
                        rs.close();
                        pstmt.close();
                        mainHandler.post(() -> callback.onSuccess(list));
                    } catch (SQLException e) {
                        mainHandler.post(() -> callback.onError(e));
                    } finally {
                        helper.releaseConnection(conn);
                    }
                }

                @Override
                public void onError(SQLException e) {
                    mainHandler.post(() -> callback.onError(e));
                }
            });
        }).start();
    }

    public void clearHistoryAsync(String userName, Runnable onDone) {
        new Thread(() -> {
            helper.getConnection(new MySQLHelper.ConnectionResultCallback() {
                @Override
                public void onSuccess(Connection conn) {
                    try {
                        String sql = "DELETE FROM search_history WHERE userName=?";
                        PreparedStatement pstmt = conn.prepareStatement(sql);
                        pstmt.setString(1, userName);
                        pstmt.executeUpdate();
                        pstmt.close();
                        mainHandler.post(onDone);
                    } catch (SQLException e) {
                        Log.e("SearchHistoryDao", "清空搜索历史失败: " + e.getMessage());
                        mainHandler.post(onDone);
                    } finally {
                        helper.releaseConnection(conn);
                    }
                }

                @Override
                public void onError(SQLException e) {
                    Log.e("SearchHistoryDao", "获取连接失败: " + e.getMessage());
                    mainHandler.post(onDone);
                }
            });
        }).start();
    }
}

