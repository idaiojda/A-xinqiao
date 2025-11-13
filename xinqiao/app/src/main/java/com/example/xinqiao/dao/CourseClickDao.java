package com.example.xinqiao.dao;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.example.xinqiao.mysql.MySQLHelper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 课程点击热度数据访问：基于 MySQL 的 videoplaylist 表，按章节（chapterId）聚合点击次数。
 */
public class CourseClickDao {
    private final MySQLHelper helper;
    private final Handler mainHandler;

    public CourseClickDao(Context context) {
        this.helper = MySQLHelper.getInstance();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /** 排行条目：章节ID与点击次数 */
    public static class ChapterCount {
        public final int chapterId;
        public final int count;
        public ChapterCount(int chapterId, int count) {
            this.chapterId = chapterId;
            this.count = count;
        }
    }

    /** 异步查询排行榜回调 */
    public interface ChapterCountCallback {
        void onSuccess(List<ChapterCount> list);
        void onError(Exception e);
    }

    /**
     * 获取全局课程点击热度排行榜（按 chapterId 聚合计数）。
     * 来源表：videoplaylist（字段：userName, chapterId, videoId, ...）
     */
    public void getGlobalCourseClickRankAsync(int limit, final ChapterCountCallback callback) {
        new Thread(() -> {
            helper.getConnection(new MySQLHelper.ConnectionResultCallback() {
                @Override
                public void onSuccess(Connection conn) {
                    List<ChapterCount> list = new ArrayList<>();
                    try {
                        String sql = "SELECT chapterId, COUNT(*) AS cnt FROM videoplaylist GROUP BY chapterId ORDER BY cnt DESC LIMIT ?";
                        PreparedStatement pstmt = conn.prepareStatement(sql);
                        pstmt.setInt(1, limit);
                        ResultSet rs = pstmt.executeQuery();
                        while (rs.next()) {
                            list.add(new ChapterCount(rs.getInt("chapterId"), rs.getInt("cnt")));
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
}

