package com.example.xinqiao.utils;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import com.example.xinqiao.mysql.MySQLHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PaymentUtils {
    private Context context;
    private MySQLHelper helper;
    private Handler mainHandler;
    private double currentBalance;

    public interface PaymentCallback {
        void onSuccess();
        void onError(String message);
    }

    /**
     * 获取当前余额数值
     */
    public double getCurrentBalance() {
        return currentBalance;
    }

    public PaymentUtils(Context context) {
        this.context = context;
        this.helper = MySQLHelper.getInstance();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }




    /**
     * 用户充值
     */
    public void recharge(String username, double amount, PaymentCallback callback) {
        new Thread(() -> {
            Connection conn = null;
            try {
                conn = helper.getConnection();
                String sql = "UPDATE user_info SET balance = balance + ? WHERE username = ?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setDouble(1, amount);
                stmt.setString(2, username);
                int result = stmt.executeUpdate();

                if (result > 0) {
                    mainHandler.post(callback::onSuccess);
                } else {
                    mainHandler.post(() -> callback.onError("充值失败，用户不存在"));
                }
            } catch (SQLException e) {
                mainHandler.post(() -> callback.onError("充值失败：" + e.getMessage()));
            } finally {
                if (conn != null) {
                    helper.releaseConnection(conn);
                }
            }
        }).start();
    }

    /**
     * 获取用户余额
     */
    public void getBalance(String username, PaymentCallback callback) {
        new Thread(() -> {
            Connection conn = null;
            try {
                conn = helper.getConnection();
                String sql = "SELECT COALESCE(balance, 0.00) as balance FROM user_info WHERE username = ?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, username);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    currentBalance = rs.getDouble("balance");
                    mainHandler.post(callback::onSuccess);
                } else {
                    currentBalance = 0.00;
                    mainHandler.post(() -> callback.onError("获取余额失败，用户不存在"));
                }
            } catch (SQLException e) {
                currentBalance = 0.00;
                mainHandler.post(() -> callback.onError("获取余额失败：" + e.getMessage()));
            } finally {
                if (conn != null) {
                    helper.releaseConnection(conn);
                }
            }
        }).start();
    }

    /**
     * 购买课程
     */
    public void purchaseCourse(String username, int courseId, PaymentCallback callback) {
        new Thread(() -> {
            Connection conn = null;
            try {
                conn = helper.getConnection();
                conn.setAutoCommit(false);

                // 检查课程价格
                String priceSql = "SELECT price FROM course_price WHERE course_id = ?";
                PreparedStatement priceStmt = conn.prepareStatement(priceSql);
                priceStmt.setInt(1, courseId);
                ResultSet priceRs = priceStmt.executeQuery();

                if (!priceRs.next()) {
                    mainHandler.post(() -> callback.onError("课程不存在"));
                    return;
                }

                double coursePrice = priceRs.getDouble("price");

                // 获取用户ID和余额
                String userSql = "SELECT user_id, balance FROM user_info WHERE username = ?";
                PreparedStatement userStmt = conn.prepareStatement(userSql);
                userStmt.setString(1, username);
                ResultSet userRs = userStmt.executeQuery();

                if (!userRs.next()) {
                    mainHandler.post(() -> callback.onError("用户不存在"));
                    return;
                }

                int userId = userRs.getInt("user_id");
                double balance = userRs.getDouble("balance");

                if (balance < coursePrice) {
                    mainHandler.post(() -> callback.onError("余额不足"));
                    return;
                }

                // 扣除余额
                String updateBalanceSql = "UPDATE user_info SET balance = balance - ? WHERE user_id = ?";
                PreparedStatement updateBalanceStmt = conn.prepareStatement(updateBalanceSql);
                updateBalanceStmt.setDouble(1, coursePrice);
                updateBalanceStmt.setInt(2, userId);
                updateBalanceStmt.executeUpdate();

                // 记录购买记录
                String purchaseSql = "INSERT INTO course_purchase (user_id, course_id, price) VALUES (?, ?, ?)";
                PreparedStatement purchaseStmt = conn.prepareStatement(purchaseSql);
                purchaseStmt.setInt(1, userId);
                purchaseStmt.setInt(2, courseId);
                purchaseStmt.setDouble(3, coursePrice);
                purchaseStmt.executeUpdate();

                conn.commit();
                mainHandler.post(callback::onSuccess);

            } catch (SQLException e) {
                try {
                    if (conn != null) {
                        conn.rollback();
                    }
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
                mainHandler.post(() -> callback.onError("购买失败：" + e.getMessage()));
            } finally {
                try {
                    if (conn != null) {
                        conn.setAutoCommit(true);
                        helper.releaseConnection(conn);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * 检查用户是否已购买课程
     */
    public void checkCoursePurchased(String username, int courseId, PaymentCallback callback) {
        new Thread(() -> {
            Connection conn = null;
            try {
                conn = helper.getConnection();
                String sql = "SELECT cp.purchase_id FROM course_purchase cp " +
                        "JOIN user_info ui ON cp.user_id = ui.user_id " +
                        "WHERE ui.username = ? AND cp.course_id = ?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, username);
                stmt.setInt(2, courseId);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    mainHandler.post(callback::onSuccess);
                } else {
                    mainHandler.post(() -> callback.onError("未购买此课程"));
                }
            } catch (SQLException e) {
                mainHandler.post(() -> callback.onError("检查购买状态失败：" + e.getMessage()));
            } finally {
                if (conn != null) {
                    helper.releaseConnection(conn);
                }
            }
        }).start();
    }
    
    /**
     * 获取最后一次查询的余额
     */
    public double getLastBalance() {
        return currentBalance;
    }
    
    /**
     * 扣除用户余额
     * @param username 用户名
     * @param amount 扣除金额
     * @param callback 回调接口
     */
    public void deductBalance(String username, double amount, PaymentCallback callback) {
        new Thread(() -> {
            Connection conn = null;
            try {
                conn = helper.getConnection();
                conn.setAutoCommit(false);

                // 获取用户ID和余额
                String userSql = "SELECT user_id, balance FROM user_info WHERE username = ?";
                PreparedStatement userStmt = conn.prepareStatement(userSql);
                userStmt.setString(1, username);
                ResultSet userRs = userStmt.executeQuery();

                if (!userRs.next()) {
                    mainHandler.post(() -> callback.onError("用户不存在"));
                    return;
                }

                int userId = userRs.getInt("user_id");
                double balance = userRs.getDouble("balance");

                if (balance < amount) {
                    mainHandler.post(() -> callback.onError("余额不足"));
                    return;
                }

                // 扣除余额
                String updateBalanceSql = "UPDATE user_info SET balance = balance - ? WHERE user_id = ?";
                PreparedStatement updateBalanceStmt = conn.prepareStatement(updateBalanceSql);
                updateBalanceStmt.setDouble(1, amount);
                updateBalanceStmt.setInt(2, userId);
                updateBalanceStmt.executeUpdate();

                // 记录交易记录
                String transactionSql = "INSERT INTO payment_transaction (user_id, amount, transaction_type, transaction_time) VALUES (?, ?, ?, NOW())";
                PreparedStatement transactionStmt = conn.prepareStatement(transactionSql);
                transactionStmt.setInt(1, userId);
                transactionStmt.setDouble(2, amount);
                transactionStmt.setString(3, "report_payment"); // 交易类型：报告支付
                transactionStmt.executeUpdate();

                conn.commit();
                currentBalance = balance - amount; // 更新当前余额
                mainHandler.post(callback::onSuccess);

            } catch (SQLException e) {
                try {
                    if (conn != null) {
                        conn.rollback();
                    }
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
                mainHandler.post(() -> callback.onError("扣款失败：" + e.getMessage()));
            } finally {
                try {
                    if (conn != null) {
                        conn.setAutoCommit(true);
                        helper.releaseConnection(conn);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    
    /**
     * 购买习题
     */
    public void purchaseExercise(String username, int exerciseId, double price, PaymentCallback callback) {
        new Thread(() -> {
            Connection conn = null;
            try {
                conn = helper.getConnection();
                conn.setAutoCommit(false);

                // 获取用户ID和余额
                String userSql = "SELECT user_id, balance FROM user_info WHERE username = ?";
                PreparedStatement userStmt = conn.prepareStatement(userSql);
                userStmt.setString(1, username);
                ResultSet userRs = userStmt.executeQuery();

                if (!userRs.next()) {
                    mainHandler.post(() -> callback.onError("用户不存在"));
                    return;
                }

                int userId = userRs.getInt("user_id");
                double balance = userRs.getDouble("balance");

                if (balance < price) {
                    mainHandler.post(() -> callback.onError("余额不足"));
                    return;
                }

                // 扣除余额
                String updateBalanceSql = "UPDATE user_info SET balance = balance - ? WHERE user_id = ?";
                PreparedStatement updateBalanceStmt = conn.prepareStatement(updateBalanceSql);
                updateBalanceStmt.setDouble(1, price);
                updateBalanceStmt.setInt(2, userId);
                updateBalanceStmt.executeUpdate();

                // 记录购买记录
                String purchaseSql = "INSERT INTO exercise_purchase (user_id, exercise_id, price) VALUES (?, ?, ?)";
                PreparedStatement purchaseStmt = conn.prepareStatement(purchaseSql);
                purchaseStmt.setInt(1, userId);
                purchaseStmt.setInt(2, exerciseId);
                purchaseStmt.setDouble(3, price);
                purchaseStmt.executeUpdate();

                conn.commit();
                currentBalance = balance - price; // 更新当前余额
                mainHandler.post(callback::onSuccess);

            } catch (SQLException e) {
                try {
                    if (conn != null) {
                        conn.rollback();
                    }
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
                mainHandler.post(() -> callback.onError("购买失败：" + e.getMessage()));
            } finally {
                try {
                    if (conn != null) {
                        conn.setAutoCommit(true);
                        helper.releaseConnection(conn);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    
    /**
     * 检查用户是否已购买习题
     */
    public void checkExercisePurchased(String username, int exerciseId, PaymentCallback callback) {
        new Thread(() -> {
            Connection conn = null;
            try {
                conn = helper.getConnection();
                String sql = "SELECT ep.purchase_id FROM exercise_purchase ep " +
                        "JOIN user_info ui ON ep.user_id = ui.user_id " +
                        "WHERE ui.username = ? AND ep.exercise_id = ?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, username);
                stmt.setInt(2, exerciseId);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    mainHandler.post(callback::onSuccess);
                } else {
                    mainHandler.post(() -> callback.onError("未购买此习题"));
                }
            } catch (SQLException e) {
                mainHandler.post(() -> callback.onError("检查购买状态失败：" + e.getMessage()));
            } finally {
                if (conn != null) {
                    helper.releaseConnection(conn);
                }
            }
        }).start();
    }
}