package com.example.xinqiao.room.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.xinqiao.room.entities.TestReportEntity;

import java.util.List;

@Dao
public interface TestReportDao {
    @Query("SELECT * FROM test_reports WHERE userName = :user ORDER BY date DESC")
    List<TestReportEntity> getAll(String user);

    @Query("SELECT * FROM test_reports WHERE userName = :user AND type = :type ORDER BY date DESC")
    List<TestReportEntity> getByType(String user, String type);

    @Query("SELECT * FROM test_reports WHERE userName = :user AND date BETWEEN :start AND :end ORDER BY date DESC")
    List<TestReportEntity> getByDateRange(String user, String start, String end);

    @Query("SELECT * FROM test_reports WHERE userName = :user AND reportId = :reportId LIMIT 1")
    TestReportEntity getByReportId(String user, String reportId);

    // 兜底：不区分用户，仅按 reportId 查询一条报告
    @Query("SELECT * FROM test_reports WHERE reportId = :reportId LIMIT 1")
    TestReportEntity getByReportIdAnyUser(String reportId);

    @Insert
    long insert(TestReportEntity entity);

    // 更新指定报告的详情，用于修复加密失败导致的内容缺失
    @Query("UPDATE test_reports SET detailsEncrypted = :details WHERE reportId = :reportId")
    int updateDetails(String reportId, String details);
}
