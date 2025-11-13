package com.example.xinqiao.bean;

public class EmotionEntry {
    public long id;
    public String date; // yyyy-MM-dd HH:mm
    public int mood;    // 0-10
    public String note;

    public EmotionEntry() {}

    public EmotionEntry(long id, String date, int mood, String note) {
        this.id = id;
        this.date = date;
        this.mood = mood;
        this.note = note;
    }
}
