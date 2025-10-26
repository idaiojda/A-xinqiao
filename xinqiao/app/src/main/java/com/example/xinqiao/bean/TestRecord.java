package com.example.xinqiao.bean;

import java.io.Serializable;

public class TestRecord implements Serializable {
    public String title;
    public String desc;
    public int imageResId;
    public String date;
    public int status; // 0未完成 1已完成 2待支付
    public boolean isFree;
    public String reportId;
    public int currentIndex; // 当前答到第几题
    public String answers; // 用户已答题目序列化（如逗号分隔的选项索引）
    // 可扩展更多字段
} 