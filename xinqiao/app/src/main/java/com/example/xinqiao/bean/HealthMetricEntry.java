package com.example.xinqiao.bean;

public class HealthMetricEntry {
    public long id;
    public String date;  // yyyy-MM-dd HH:mm
    public String type;  // 指标类型（心率/血压/体温等）
    public float value;  // 数值
    public String unit;  // 单位

    public HealthMetricEntry() {}

    public HealthMetricEntry(long id, String date, String type, float value, String unit) {
        this.id = id;
        this.date = date;
        this.type = type;
        this.value = value;
        this.unit = unit;
    }
}
