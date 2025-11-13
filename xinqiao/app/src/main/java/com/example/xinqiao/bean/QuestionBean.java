package com.example.xinqiao.bean;

import java.io.Serializable;
 
public class QuestionBean implements Serializable {
    public String question; // 题干
    public String[] options; // 选项
    public int correctIndex; // 正确答案索引（0~3）

    // 新增：带参数构造方法
    public QuestionBean(String question, String[] options, int correctIndex) {
        this.question = question;
        this.options = options;
        this.correctIndex = correctIndex;
    }

    // 新增：无参构造方法，兼容老代码
    public QuestionBean() {}
} 