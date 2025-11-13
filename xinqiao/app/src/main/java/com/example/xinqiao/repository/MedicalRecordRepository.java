package com.example.xinqiao.repository;

import android.content.Context;

import com.example.xinqiao.room.XQMedicalRecordDatabase;
import com.example.xinqiao.room.dao.AuthorizationDao;
import com.example.xinqiao.room.dao.ConsultationDao;
import com.example.xinqiao.room.dao.EmotionDiaryDao;
import com.example.xinqiao.room.dao.TestReportDao;
import com.example.xinqiao.room.entities.AuthorizationEntity;
import com.example.xinqiao.room.entities.ConsultationEntity;
import com.example.xinqiao.room.entities.EmotionDiaryEntity;
import com.example.xinqiao.room.entities.TestReportEntity;
import com.example.xinqiao.util.CryptoUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository with simple 24h TTL cache over Room for medical records.
 */
public class MedicalRecordRepository {

    private static final long TTL_24H = 24L * 60L * 60L * 1000L;

    private final EmotionDiaryDao emotionDiaryDao;
    private final ConsultationDao consultationDao;
    private final TestReportDao testReportDao;
    private final AuthorizationDao authorizationDao;

    private final Map<String, Long> lastFetchAt = new HashMap<>();
    private List<EmotionDiaryEntity> cacheDiaries = Collections.emptyList();
    private List<ConsultationEntity> cacheConsultations = Collections.emptyList();
    private List<TestReportEntity> cacheReports = Collections.emptyList();
    private List<AuthorizationEntity> cacheAuthorizations = Collections.emptyList();

    public MedicalRecordRepository(Context context) {
        XQMedicalRecordDatabase db = XQMedicalRecordDatabase.getInstance(context);
        this.emotionDiaryDao = db.emotionDiaryDao();
        this.consultationDao = db.consultationDao();
        this.testReportDao = db.testReportDao();
        this.authorizationDao = db.authorizationDao();
    }

    private boolean isFresh(String key) {
        Long ts = lastFetchAt.get(key);
        return ts != null && (System.currentTimeMillis() - ts) < TTL_24H;
    }

    private void markFresh(String key) { lastFetchAt.put(key, System.currentTimeMillis()); }

    // Emotion Diaries
    public List<EmotionDiaryEntity> getEmotionDiaries(String userName, boolean useCache) {
        String key = "diaries:" + userName;
        if (useCache && isFresh(key)) return cacheDiaries;
        cacheDiaries = emotionDiaryDao.getAll(userName);
        markFresh(key);
        return cacheDiaries;
    }

    // 异步查询：避免在主线程进行 Room 查询导致崩溃
    public interface EmotionDiariesCallback {
        void onSuccess(List<EmotionDiaryEntity> list);
        void onError(Exception e);
    }

    public void getEmotionDiariesAsync(String userName, boolean useCache, EmotionDiariesCallback callback) {
        new Thread(() -> {
            try {
                String key = "diaries:" + userName;
                List<EmotionDiaryEntity> list;
                if (useCache && isFresh(key)) {
                    list = cacheDiaries;
                } else {
                    list = emotionDiaryDao.getAll(userName);
                    cacheDiaries = list;
                    markFresh(key);
                }
                if (callback != null) callback.onSuccess(list);
            } catch (Exception e) {
                if (callback != null) callback.onError(e);
            }
        }).start();
    }

    public List<EmotionDiaryEntity> getEmotionDiariesByDateRange(String userName, String start, String end) {
        // direct query without cache TTL, as ranges can vary
        return emotionDiaryDao.getByDateRange(userName, start, end);
    }

    public void getEmotionDiariesByDateRangeAsync(String userName, String start, String end, EmotionDiariesCallback callback) {
        new Thread(() -> {
            try {
                List<EmotionDiaryEntity> list = emotionDiaryDao.getByDateRange(userName, start, end);
                if (callback != null) callback.onSuccess(list);
            } catch (Exception e) {
                if (callback != null) callback.onError(e);
            }
        }).start();
    }

    // 写操作异步封装，避免主线程执行事务
    public interface EmotionDiaryIdCallback {
        void onSuccess(long id);
        void onError(Exception e);
    }

    public interface EmotionDiaryRowsCallback {
        void onSuccess(int rows);
        void onError(Exception e);
    }

    public void addEmotionDiaryAsync(String userName, String date, int mood, String notePlain, EmotionDiaryIdCallback callback) {
        new Thread(() -> {
            try {
                long id = addEmotionDiary(userName, date, mood, notePlain);
                if (callback != null) callback.onSuccess(id);
            } catch (Exception e) {
                if (callback != null) callback.onError(e);
            }
        }).start();
    }

    public void deleteEmotionDiaryByIdAsync(long id, String userName, EmotionDiaryRowsCallback callback) {
        new Thread(() -> {
            try {
                int rows = deleteEmotionDiaryById(id, userName);
                if (callback != null) callback.onSuccess(rows);
            } catch (Exception e) {
                if (callback != null) callback.onError(e);
            }
        }).start();
    }

    public long addEmotionDiary(String userName, String date, int mood, String notePlain) {
        EmotionDiaryEntity e = new EmotionDiaryEntity();
        e.id = 0;
        e.userName = userName;
        e.date = date; // yyyy-MM-dd
        e.mood = mood; // 1-10
        e.noteEncrypted = CryptoUtil.encrypt(notePlain);
        long id = emotionDiaryDao.insert(e);
        // invalidate cache
        lastFetchAt.remove("diaries:" + userName);
        return id;
    }

    public int updateEmotionDiary(long id, String userName, String date, int mood, String notePlain) {
        EmotionDiaryEntity e = new EmotionDiaryEntity();
        e.id = id;
        e.userName = userName;
        e.date = date;
        e.mood = mood;
        e.noteEncrypted = CryptoUtil.encrypt(notePlain);
        int rows = emotionDiaryDao.update(e);
        // invalidate cache
        lastFetchAt.remove("diaries:" + userName);
        return rows;
    }

    public int deleteEmotionDiaryById(long id, String userName) {
        int rows = emotionDiaryDao.deleteById(id);
        lastFetchAt.remove("diaries:" + userName);
        return rows;
    }

    // Consultations
    public List<ConsultationEntity> getConsultations(String userName, boolean useCache) {
        String key = "consults:" + userName;
        if (useCache && isFresh(key)) return cacheConsultations;
        cacheConsultations = consultationDao.getAll(userName);
        markFresh(key);
        return cacheConsultations;
    }

    public List<ConsultationEntity> getConsultationsByDateRange(String userName, String start, String end) {
        return consultationDao.getByDateRange(userName, start, end);
    }

    // 异步封装，避免主线程访问数据库
    public interface ConsultationsCallback {
        void onSuccess(List<ConsultationEntity> list);
        void onError(Exception e);
    }

    public void getConsultationsAsync(String userName, boolean useCache, ConsultationsCallback callback) {
        new Thread(() -> {
            try {
                String key = "consults:" + userName;
                List<ConsultationEntity> list;
                if (useCache && isFresh(key)) {
                    list = cacheConsultations;
                } else {
                    list = consultationDao.getAll(userName);
                    cacheConsultations = list;
                    markFresh(key);
                }
                if (callback != null) callback.onSuccess(list);
            } catch (Exception e) {
                if (callback != null) callback.onError(e);
            }
        }).start();
    }

    public void getConsultationsByDateRangeAsync(String userName, String start, String end, ConsultationsCallback callback) {
        new Thread(() -> {
            try {
                List<ConsultationEntity> list = consultationDao.getByDateRange(userName, start, end);
                if (callback != null) callback.onSuccess(list);
            } catch (Exception e) {
                if (callback != null) callback.onError(e);
            }
        }).start();
    }

    public int updateConsultation(ConsultationEntity entity) {
        int rows = consultationDao.update(entity);
        lastFetchAt.remove("consults:" + entity.userName);
        return rows;
    }

    public int updateConsultationMessageCount(String userName, String sessionId, int count) {
        int rows = consultationDao.updateMessageCount(userName, sessionId, count);
        lastFetchAt.remove("consults:" + userName);
        return rows;
    }

    public long addConsultation(ConsultationEntity entity) {
        long id = consultationDao.insert(entity);
        lastFetchAt.remove("consults:" + entity.userName);
        return id;
    }
    public ConsultationEntity getConsultationBySessionId(String userName, String sessionId) {
        return consultationDao.getBySessionId(userName, sessionId);
    }

    // Test Reports
    public List<TestReportEntity> getTestReports(String userName, boolean useCache) {
        String key = "reports:" + userName;
        if (useCache && isFresh(key)) return cacheReports;
        cacheReports = testReportDao.getAll(userName);
        markFresh(key);
        return cacheReports;
    }

    public List<TestReportEntity> getTestReportsByDateRange(String userName, String start, String end) {
        return testReportDao.getByDateRange(userName, start, end);
    }

    public TestReportEntity getTestReportByReportId(String userName, String reportId) {
        return testReportDao.getByReportId(userName, reportId);
    }

    // 兜底：不区分用户，仅按 reportId 查询
    public TestReportEntity getTestReportByReportIdAnyUser(String reportId) {
        return testReportDao.getByReportIdAnyUser(reportId);
    }

    // 异步封装，避免主线程访问数据库
    public interface TestReportsCallback {
        void onSuccess(List<TestReportEntity> list);
        void onError(Exception e);
    }

    public void getTestReportsAsync(String userName, boolean useCache, TestReportsCallback callback) {
        new Thread(() -> {
            try {
                String key = "reports:" + userName;
                List<TestReportEntity> list;
                if (useCache && isFresh(key)) {
                    list = cacheReports;
                } else {
                    list = testReportDao.getAll(userName);
                    cacheReports = list;
                    markFresh(key);
                }
                if (callback != null) callback.onSuccess(list);
            } catch (Exception e) {
                if (callback != null) callback.onError(e);
            }
        }).start();
    }

    public void getTestReportsByDateRangeAsync(String userName, String start, String end, TestReportsCallback callback) {
        new Thread(() -> {
            try {
                List<TestReportEntity> list = testReportDao.getByDateRange(userName, start, end);
                if (callback != null) callback.onSuccess(list);
            } catch (Exception e) {
                if (callback != null) callback.onError(e);
            }
        }).start();
    }

    public interface TestReportEntityCallback {
        void onSuccess(TestReportEntity entity);
        void onError(Exception e);
    }

    public void getTestReportByReportIdAsync(String userName, String reportId, TestReportEntityCallback callback) {
        new Thread(() -> {
            try {
                TestReportEntity e = testReportDao.getByReportId(userName, reportId);
                if (callback != null) callback.onSuccess(e);
            } catch (Exception ex) {
                if (callback != null) callback.onError(ex);
            }
        }).start();
    }

    // 兜底异步：不区分用户，仅按 reportId 查询
    public void getTestReportByReportIdAnyUserAsync(String reportId, TestReportEntityCallback callback) {
        new Thread(() -> {
            try {
                TestReportEntity e = testReportDao.getByReportIdAnyUser(reportId);
                if (callback != null) callback.onSuccess(e);
            } catch (Exception ex) {
                if (callback != null) callback.onError(ex);
            }
        }).start();
    }

    public long addTestReport(TestReportEntity entity) {
        // 插入报告：如果上层已设置加密详情，则直接保存；不再依赖不存在的明文获取方法
        long id = testReportDao.insert(entity);
        lastFetchAt.remove("reports:" + entity.userName);
        return id;
    }

    // 可选的重载：当上层提供明文详情时，在此处进行加密再保存
    public long addTestReport(TestReportEntity entity, String detailsPlain) {
        if (entity.detailsEncrypted == null && detailsPlain != null) {
            entity.detailsEncrypted = CryptoUtil.encrypt(detailsPlain);
        }
        long id = testReportDao.insert(entity);
        lastFetchAt.remove("reports:" + entity.userName);
        return id;
    }

    /**
     * 更新指定报告的详情加密字段，用于在打开报告时回填缺失内容。
     * 若加密失败，则以 PLA: 前缀的明文Base64形式存储，保证可读性。
     */
    public int updateTestReportDetails(String reportId, String detailsPlain) {
        String encrypted = null;
        try {
            encrypted = CryptoUtil.encrypt(detailsPlain);
        } catch (Exception ignore) {}
        if (encrypted == null && detailsPlain != null) {
            try {
                encrypted = "PLA:" + android.util.Base64.encodeToString(detailsPlain.getBytes("UTF-8"), android.util.Base64.NO_WRAP);
            } catch (Exception e) {
                encrypted = "PLA:"; // 极端情况下保留前缀以避免解析异常
            }
        }
        int rows = testReportDao.updateDetails(reportId, encrypted);
        // 由于无法确定用户名，避免误清空其它缓存，这里不做cache刷新；
        // 报告详情读取使用单条查询，不受列表缓存影响。
        return rows;
    }

    // Authorizations
    public List<AuthorizationEntity> getAuthorizations(String userName, boolean useCache) {
        String key = "auths:" + userName;
        if (useCache && isFresh(key)) return cacheAuthorizations;
        cacheAuthorizations = authorizationDao.getAll(userName);
        markFresh(key);
        return cacheAuthorizations;
    }

    public long addAuthorization(AuthorizationEntity entity) {
        long id = authorizationDao.insert(entity);
        lastFetchAt.remove("auths:" + entity.userName);
        return id;
    }

    public List<AuthorizationEntity> getActiveAuthorizations(String userName, boolean useCache) {
        String key = "auths_active:" + userName;
        if (useCache && isFresh(key)) return cacheAuthorizations;
        cacheAuthorizations = authorizationDao.getActive(userName, System.currentTimeMillis());
        markFresh(key);
        return cacheAuthorizations;
    }

    public int updateAuthorization(AuthorizationEntity entity) {
        int rows = authorizationDao.update(entity);
        lastFetchAt.remove("auths:" + entity.userName);
        lastFetchAt.remove("auths_active:" + entity.userName);
        return rows;
    }
}
