package com.example.xinqiao.util.storage;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.xinqiao.bean.EmotionEntry;
import com.example.xinqiao.bean.HealthMetricEntry;
import com.example.xinqiao.bean.AIConsultationEntry;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MedicalRecordStorage {
    private static final String PREF_NAME = "medical_record";
    private static final String KEY_EMOTIONS = "emotion_entries";
    private static final String KEY_METRICS = "health_metrics_entries";
    private static final String KEY_AI_CONSULTATIONS = "ai_consultation_entries";

    // Emotion Diary
    public static List<EmotionEntry> loadEmotionEntries(Context context) {
        List<EmotionEntry> list = new ArrayList<>();
        try {
            SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String json = sp.getString(KEY_EMOTIONS, "[]");
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                EmotionEntry e = new EmotionEntry();
                e.id = o.optLong("id", System.currentTimeMillis());
                e.date = o.optString("date", "");
                e.mood = o.optInt("mood", 5);
                e.note = o.optString("note", "");
                list.add(e);
            }
        } catch (Exception ignore) {}
        return list;
    }

    public static void saveEmotionEntries(Context context, List<EmotionEntry> list) {
        try {
            JSONArray arr = new JSONArray();
            if (list != null) {
                for (EmotionEntry e : list) {
                    JSONObject o = new JSONObject();
                    o.put("id", e.id);
                    o.put("date", e.date);
                    o.put("mood", e.mood);
                    o.put("note", e.note);
                    arr.put(o);
                }
            }
            SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            sp.edit().putString(KEY_EMOTIONS, arr.toString()).apply();
        } catch (Exception ignore) {}
    }

    // Health Metrics
    public static List<HealthMetricEntry> loadHealthMetricEntries(Context context) {
        List<HealthMetricEntry> list = new ArrayList<>();
        try {
            SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String json = sp.getString(KEY_METRICS, "[]");
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                HealthMetricEntry e = new HealthMetricEntry();
                e.id = o.optLong("id", System.currentTimeMillis());
                e.date = o.optString("date", "");
                e.type = o.optString("type", "");
                e.value = (float) o.optDouble("value", 0.0);
                e.unit = o.optString("unit", "");
                list.add(e);
            }
        } catch (Exception ignore) {}
        return list;
    }

    public static void saveHealthMetricEntries(Context context, List<HealthMetricEntry> list) {
        try {
            JSONArray arr = new JSONArray();
            if (list != null) {
                for (HealthMetricEntry e : list) {
                    JSONObject o = new JSONObject();
                    o.put("id", e.id);
                    o.put("date", e.date);
                    o.put("type", e.type);
                    o.put("value", e.value);
                    o.put("unit", e.unit);
                    arr.put(o);
                }
            }
            SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            sp.edit().putString(KEY_METRICS, arr.toString()).apply();
        } catch (Exception ignore) {}
    }

    // AI 咨询记录
    public static List<AIConsultationEntry> loadAIConsultationEntries(Context context) {
        List<AIConsultationEntry> list = new ArrayList<>();
        try {
            SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String json = sp.getString(KEY_AI_CONSULTATIONS, "[]");
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                AIConsultationEntry e = new AIConsultationEntry();
                e.id = o.optLong("id", System.currentTimeMillis());
                e.userName = o.optString("userName", "");
                e.date = o.optString("date", "");
                e.sessionId = o.optInt("sessionId", 0);
                e.title = o.optString("title", "新对话");
                e.messageCount = o.optInt("messageCount", 0);
                list.add(e);
            }
        } catch (Exception ignore) {}
        return list;
    }

    public static void saveAIConsultationEntries(Context context, List<AIConsultationEntry> list) {
        try {
            JSONArray arr = new JSONArray();
            if (list != null) {
                for (AIConsultationEntry e : list) {
                    JSONObject o = new JSONObject();
                    o.put("id", e.id);
                    o.put("userName", e.userName);
                    o.put("date", e.date);
                    o.put("sessionId", e.sessionId);
                    o.put("title", e.title);
                    o.put("messageCount", e.messageCount);
                    arr.put(o);
                }
            }
            SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            sp.edit().putString(KEY_AI_CONSULTATIONS, arr.toString()).apply();
        } catch (Exception ignore) {}
    }
}
