package com.example.xinqiao.room;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.xinqiao.room.dao.AuthorizationDao;
import com.example.xinqiao.room.dao.ConsultationDao;
import com.example.xinqiao.room.dao.EmotionDiaryDao;
import com.example.xinqiao.room.dao.TestReportDao;
import com.example.xinqiao.room.entities.AuthorizationEntity;
import com.example.xinqiao.room.entities.ConsultationEntity;
import com.example.xinqiao.room.entities.EmotionDiaryEntity;
import com.example.xinqiao.room.entities.TestReportEntity;

@Database(
        entities = {
                EmotionDiaryEntity.class,
                ConsultationEntity.class,
                TestReportEntity.class,
                AuthorizationEntity.class
        },
        version = 1,
        exportSchema = false
)
public abstract class XQMedicalRecordDatabase extends RoomDatabase {

    public abstract EmotionDiaryDao emotionDiaryDao();
    public abstract ConsultationDao consultationDao();
    public abstract TestReportDao testReportDao();
    public abstract AuthorizationDao authorizationDao();

    private static volatile XQMedicalRecordDatabase INSTANCE;

    public static XQMedicalRecordDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (XQMedicalRecordDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    XQMedicalRecordDatabase.class,
                                    "xq_medical_record.db"
                            )
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}

