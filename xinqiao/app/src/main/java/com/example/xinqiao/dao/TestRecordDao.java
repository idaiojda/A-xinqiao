package com.example.xinqiao.dao;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.example.xinqiao.bean.TestRecord;
import com.example.xinqiao.mysql.MySQLHelper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TestRecordDao {
    private Context context;
    private MySQLHelper helper;
    private Handler mainHandler;
    
    public TestRecordDao(Context context) {
        this.context = context;
        helper = MySQLHelper.getInstance();
        mainHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * 异步查询回调接口
     */
    public interface TestRecordCallback {
        void onSuccess(List<TestRecord> records);
        void onError(Exception e);
    }
    // 新增测评记录
    public boolean saveTestRecord(String userName, TestRecord record) {
        boolean flag = false;
        Connection conn = null;
        try {
            conn = helper.getConnection();
            
            // 开始事务
            conn.setAutoCommit(false);
            
            try {
                // 先删除相同reportId的记录（无论是否存在）
                android.util.Log.d("TestRecordDao", "删除可能存在的重复记录: reportId=" + record.reportId);
                String deleteSql = "DELETE FROM test_record WHERE reportId = ?";
                PreparedStatement deleteStmt = conn.prepareStatement(deleteSql);
                deleteStmt.setString(1, record.reportId);
                deleteStmt.executeUpdate();
                deleteStmt.close();
                
                // 插入新记录
                String sql = "INSERT INTO test_record (userName, title, `desc`, imageResId, date, status, isFree, reportId, currentIndex, answers) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, userName);
                pstmt.setString(2, record.title);
                pstmt.setString(3, record.desc);
                pstmt.setInt(4, record.imageResId);
                pstmt.setString(5, record.date);
                pstmt.setInt(6, record.status);
                pstmt.setBoolean(7, record.isFree);
                pstmt.setString(8, record.reportId);
                pstmt.setInt(9, record.currentIndex);
                pstmt.setString(10, record.answers);
                int result = pstmt.executeUpdate();
                if (result > 0) flag = true;
                pstmt.close();
                
                // 提交事务
                conn.commit();
                android.util.Log.d("TestRecordDao", "事务提交成功: reportId=" + record.reportId);
                
            } catch (SQLException e) {
                // 回滚事务
                try {
                    if (conn != null) {
                        conn.rollback();
                        android.util.Log.e("TestRecordDao", "事务回滚: " + e.getMessage());
                    }
                } catch (SQLException rollbackEx) {
                    android.util.Log.e("TestRecordDao", "事务回滚失败: " + rollbackEx.getMessage());
                }
                e.printStackTrace();
                flag = false;
            } finally {
                // 恢复自动提交
                try {
                    if (conn != null) {
                        conn.setAutoCommit(true);
                    }
                } catch (SQLException autoCommitEx) {
                    android.util.Log.e("TestRecordDao", "恢复自动提交失败: " + autoCommitEx.getMessage());
                }
                if (conn != null) helper.releaseConnection(conn);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            flag = false;
        }
        return flag;
    }
    // 查询当前用户所有测评记录
    public List<TestRecord> getTestRecords(String userName) {
        List<TestRecord> list = new ArrayList<>();
        String sql = "SELECT * FROM test_record WHERE userName=? ORDER BY date DESC";
        try (Connection conn = helper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userName);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                TestRecord r = new TestRecord();
                r.title = rs.getString("title");
                r.desc = rs.getString("desc");
                r.imageResId = rs.getInt("imageResId");
                r.date = rs.getString("date");
                r.status = rs.getInt("status");
                r.isFree = rs.getBoolean("isFree");
                r.reportId = rs.getString("reportId");
                r.currentIndex = rs.getInt("currentIndex");
                r.answers = rs.getString("answers");
                list.add(r);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
    
    /**
     * 异步查询所有测评记录
     * @param userName 用户名
     * @param callback 回调接口
     */
    public void getTestRecordsAsync(String userName, final TestRecordCallback callback) {
        new Thread(() -> {
            helper.getConnection(new MySQLHelper.ConnectionResultCallback() {
                @Override
                public void onSuccess(Connection conn) {
                    List<TestRecord> list = new ArrayList<>();
                    try {
                        String sql = "SELECT * FROM test_record WHERE userName=? ORDER BY date DESC";
                        PreparedStatement pstmt = conn.prepareStatement(sql);
                        pstmt.setString(1, userName);
                        ResultSet rs = pstmt.executeQuery();
                        
                        while (rs.next()) {
                            TestRecord r = new TestRecord();
                            r.title = rs.getString("title");
                            r.desc = rs.getString("desc");
                            r.imageResId = rs.getInt("imageResId");
                            r.date = rs.getString("date");
                            r.status = rs.getInt("status");
                            r.isFree = rs.getBoolean("isFree");
                            r.reportId = rs.getString("reportId");
                            r.currentIndex = rs.getInt("currentIndex");
                            r.answers = rs.getString("answers");
                            list.add(r);
                        }
                        
                        rs.close();
                        pstmt.close();
                        
                        // 在主线程中回调结果
                        mainHandler.post(() -> callback.onSuccess(list));
                    } catch (SQLException e) {
                        android.util.Log.e("TestRecordDao", "查询所有测评记录失败: " + e.getMessage());
                        mainHandler.post(() -> callback.onError(e));
                    } finally {
                        helper.releaseConnection(conn);
                    }
                }
                
                @Override
                public void onError(SQLException e) {
                    android.util.Log.e("TestRecordDao", "获取数据库连接失败: " + e.getMessage());
                    mainHandler.post(() -> callback.onError(e));
                }
            });
        }).start();
    }
    // 按状态查
    public List<TestRecord> getTestRecordsByStatus(String userName, int status) {
        List<TestRecord> list = new ArrayList<>();
        String sql = "SELECT * FROM test_record WHERE userName=? AND status=? ORDER BY date DESC";
        try (Connection conn = helper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userName);
            pstmt.setInt(2, status);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                TestRecord r = new TestRecord();
                r.title = rs.getString("title");
                r.desc = rs.getString("desc");
                r.imageResId = rs.getInt("imageResId");
                r.date = rs.getString("date");
                r.status = rs.getInt("status");
                r.isFree = rs.getBoolean("isFree");
                r.reportId = rs.getString("reportId");
                r.currentIndex = rs.getInt("currentIndex");
                r.answers = rs.getString("answers");
                list.add(r);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
    
    /**
     * 异步按状态查询测评记录
     * @param userName 用户名
     * @param status 状态码
     * @param callback 回调接口
     */
    public void getTestRecordsByStatusAsync(String userName, int status, final TestRecordCallback callback) {
        new Thread(() -> {
            helper.getConnection(new MySQLHelper.ConnectionResultCallback() {
                @Override
                public void onSuccess(Connection conn) {
                    List<TestRecord> list = new ArrayList<>();
                    try {
                        String sql;
                        PreparedStatement pstmt;
                        
                        // 如果查询未完成记录，需要排除已经有完成记录的测试项目
                        if (status == 0) {
                            // 查询未完成记录，但排除那些已经有完成记录的测试项目
                            // 使用子查询获取每个title的最新记录
                            sql = "SELECT t1.* FROM test_record t1 " +
                                  "JOIN (" +
                                  "  SELECT title, MAX(date) as max_date " +
                                  "  FROM test_record " +
                                  "  WHERE userName=? AND status=0 " +
                                  "  GROUP BY title" +
                                  ") t3 ON t1.title=t3.title AND t1.date=t3.max_date " +
                                  "WHERE t1.userName=? AND t1.status=0 " +
                                  "AND NOT EXISTS (" +
                                  "  SELECT 1 FROM test_record t2 " +
                                  "  WHERE t2.userName=t1.userName AND t2.title=t1.title AND t2.status=1" +
                                  ") " +
                                  "ORDER BY t1.date DESC";
                            pstmt = conn.prepareStatement(sql);
                            pstmt.setString(1, userName); // 子查询参数
                            pstmt.setString(2, userName); // 主查询参数
                        } else {
                            // 其他状态的记录正常查询，但确保每个title只返回一条最新记录
                            sql = "SELECT t1.* FROM test_record t1 " +
                                  "JOIN (" +
                                  "  SELECT title, MAX(date) as max_date " +
                                  "  FROM test_record " +
                                  "  WHERE userName=? AND status=? " +
                                  "  GROUP BY title" +
                                  ") t2 ON t1.title=t2.title AND t1.date=t2.max_date " +
                                  "WHERE t1.userName=? AND t1.status=? " +
                                  "ORDER BY t1.date DESC";
                            pstmt = conn.prepareStatement(sql);
                            pstmt.setString(1, userName); // 子查询参数
                            pstmt.setInt(2, status);    // 子查询参数
                            pstmt.setString(3, userName); // 主查询参数
                            pstmt.setInt(4, status);    // 主查询参数
                        }
                        
                        ResultSet rs = pstmt.executeQuery();
                        
                        while (rs.next()) {
                            TestRecord r = new TestRecord();
                            r.title = rs.getString("title");
                            r.desc = rs.getString("desc");
                            r.imageResId = rs.getInt("imageResId");
                            r.date = rs.getString("date");
                            r.status = rs.getInt("status");
                            r.isFree = rs.getBoolean("isFree");
                            r.reportId = rs.getString("reportId");
                            r.currentIndex = rs.getInt("currentIndex");
                            r.answers = rs.getString("answers");
                            list.add(r);
                        }
                        
                        rs.close();
                        pstmt.close();
                        
                        // 在主线程中回调结果
                        mainHandler.post(() -> callback.onSuccess(list));
                    } catch (SQLException e) {
                        android.util.Log.e("TestRecordDao", "查询测评记录失败: " + e.getMessage());
                        mainHandler.post(() -> callback.onError(e));
                    } finally {
                        helper.releaseConnection(conn);
                    }
                }
                
                @Override
                public void onError(SQLException e) {
                    android.util.Log.e("TestRecordDao", "获取数据库连接失败: " + e.getMessage());
                    mainHandler.post(() -> callback.onError(e));
                }
            });
        }).start();
    }
    // 新增：更新测评记录状态
    public boolean updateTestRecordStatus(String reportId, int newStatus) {
        boolean flag = false;
        Connection conn = null;
        try {
            conn = helper.getConnection();
            String sql = "UPDATE test_record SET status=? WHERE reportId=?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, newStatus);
            pstmt.setString(2, reportId);
            int result = pstmt.executeUpdate();
            if (result > 0) flag = true;
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (conn != null) helper.releaseConnection(conn);
        }
        return flag;
    }
    
    /**
     * 获取测评记录状态
     * @param reportId 报告ID
     * @return 状态码：0未完成 1已完成 2待支付，如果记录不存在返回-1
     */
    public int getTestRecordStatus(String reportId) {
        int status = -1;
        Connection conn = null;
        try {
            conn = helper.getConnection();
            String sql = "SELECT status FROM test_record WHERE reportId=?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, reportId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                status = rs.getInt("status");
            }
            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (conn != null) helper.releaseConnection(conn);
        }
        return status;
    }
    
    // 新增：删除指定reportId的记录
    public boolean deleteTestRecordById(String reportId) {
        boolean flag = false;
        Connection conn = null;
        try {
            conn = helper.getConnection();
            String sql = "DELETE FROM test_record WHERE reportId=?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, reportId);
            int result = pstmt.executeUpdate();
            if (result > 0) flag = true;
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (conn != null) helper.releaseConnection(conn);
        }
        return flag;
    }
}