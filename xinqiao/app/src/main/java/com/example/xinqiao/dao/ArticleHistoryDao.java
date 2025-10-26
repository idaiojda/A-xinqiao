package com.example.xinqiao.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.example.xinqiao.bean.ArticleBean;
import com.example.xinqiao.mysql.MySQLHelper;

import java.util.ArrayList;
import java.util.List;

public class ArticleHistoryDao {
    private Context context;
    private MySQLHelper helper;

    public ArticleHistoryDao(Context context) {
        this.context = context;
        helper = MySQLHelper.getInstance();
    }

    /**
     * 保存文章阅读记录
     */
    public boolean saveArticleHistory(ArticleBean article) {
        boolean flag = false;
        Connection conn = null;
        try {
            conn = helper.getConnection();
            String sql = "INSERT INTO article_history (userName, articleId, title, content, category, readTimestamp, readProgress) VALUES (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, article.userName);
            pstmt.setInt(2, article.articleId);
            pstmt.setString(3, article.title);
            pstmt.setString(4, article.content);
            pstmt.setString(5, article.category);
            pstmt.setLong(6, article.readTimestamp);
            pstmt.setInt(7, article.readProgress);
            int result = pstmt.executeUpdate();
            if (result > 0) {
                flag = true;
            }
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                helper.releaseConnection(conn);
            }
        }
        return flag;
    }

    /**
     * 获取用户的文章阅读历史记录
     */
    public List<ArticleBean> getArticleHistory(String userName) {
        List<ArticleBean> articleList = new ArrayList<>();
        Connection conn = null;
        try {
            conn = helper.getConnection();
            String sql = "SELECT * FROM article_history WHERE userName=? ORDER BY readTimestamp DESC";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, userName);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                ArticleBean article = new ArticleBean();
                article.articleId = rs.getInt("articleId");
                article.userName = rs.getString("userName");
                article.title = rs.getString("title");
                article.content = rs.getString("content");
                article.category = rs.getString("category");
                article.readTimestamp = rs.getLong("readTimestamp");
                article.readProgress = rs.getInt("readProgress");
                articleList.add(article);
            }
            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                helper.releaseConnection(conn);
            }
        }
        return articleList;
    }

    /**
     * 更新文章阅读进度
     */
    public boolean updateReadProgress(String userName, int articleId, int progress) {
        boolean flag = false;
        Connection conn = null;
        try {
            conn = helper.getConnection();
            String sql = "UPDATE article_history SET readProgress=?, readTimestamp=? WHERE userName=? AND articleId=?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, progress);
            pstmt.setLong(2, System.currentTimeMillis());
            pstmt.setString(3, userName);
            pstmt.setInt(4, articleId);
            int result = pstmt.executeUpdate();
            if (result > 0) {
                flag = true;
            }
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                helper.releaseConnection(conn);
            }
        }
        return flag;
    }

    /**
     * 删除用户的文章阅读历史记录
     */
    public boolean deleteArticleHistory(String userName) {
        boolean flag = false;
        Connection conn = null;
        try {
            conn = helper.getConnection();
            String sql = "DELETE FROM article_history WHERE userName=?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, userName);
            int result = pstmt.executeUpdate();
            if (result > 0) {
                flag = true;
            }
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                helper.releaseConnection(conn);
            }
        }
        return flag;
    }

    /**
     * 异步获取用户的文章阅读历史记录
     */
    public interface ArticleHistoryCallback {
        void onResult(List<ArticleBean> history);
    }

    public void getArticleHistoryAsync(String userName, ArticleHistoryCallback callback) {
        new Thread(() -> {
            List<ArticleBean> result = getArticleHistory(userName);
            android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            mainHandler.post(() -> callback.onResult(result));
        }).start();
    }

    /**
     * 保存/删除历史的异步回调
     */
    public interface SimpleResultCallback {
        void onResult(boolean success);
    }

    /**
     * 异步保存文章阅读记录
     */
    public void saveArticleHistoryAsync(ArticleBean article, SimpleResultCallback callback) {
        new Thread(() -> {
            boolean result = saveArticleHistory(article);
            android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            mainHandler.post(() -> callback.onResult(result));
        }).start();
    }

    /**
     * 异步删除用户的文章阅读历史记录
     */
    public void deleteArticleHistoryAsync(String userName, SimpleResultCallback callback) {
        new Thread(() -> {
            boolean result = deleteArticleHistory(userName);
            android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            mainHandler.post(() -> callback.onResult(result));
        }).start();
    }

    /**
     * 判断某用户某文章是否已存在历史记录
     */
    public boolean isArticleHistoryExists(String userName, int articleId) {
        Connection conn = null;
        try {
            conn = helper.getConnection();
            String sql = "SELECT COUNT(*) FROM article_history WHERE userName=? AND articleId=?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, userName);
            pstmt.setInt(2, articleId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                boolean exists = rs.getInt(1) > 0;
                rs.close();
                pstmt.close();
                return exists;
            }
            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                helper.releaseConnection(conn);
            }
        }
        return false;
    }

    /**
     * 异步判断某用户某文章是否已存在历史记录
     */
    public interface ExistsCallback {
        void onResult(boolean exists);
    }
    public void isArticleHistoryExistsAsync(String userName, int articleId, ExistsCallback callback) {
        new Thread(() -> {
            boolean exists = isArticleHistoryExists(userName, articleId);
            android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            mainHandler.post(() -> callback.onResult(exists));
        }).start();
    }
}