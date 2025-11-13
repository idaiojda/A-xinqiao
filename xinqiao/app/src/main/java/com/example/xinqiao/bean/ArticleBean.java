package com.example.xinqiao.bean;

public class ArticleBean {
    public int articleId;          // 文章ID
    public String userName;        // 用户名
    public String title;          // 文章标题
    public String content;        // 文章内容
    public String category;       // 文章分类
    public long readTimestamp;    // 阅读时间戳
    public int readProgress;      // 阅读进度（百分比）
    public String summary;        // 文章摘要
    public int imageResId;        // 文章图片资源ID
    
    public ArticleBean() {
    }
    
    public ArticleBean(int articleId, String userName, String title, String content, 
                      String category, long readTimestamp, int readProgress, String summary, int imageResId) {
        this.articleId = articleId;
        this.userName = userName;
        this.title = title;
        this.content = content;
        this.category = category;
        this.readTimestamp = readTimestamp;
        this.readProgress = readProgress;
        this.summary = summary;
        this.imageResId = imageResId;
    }
}