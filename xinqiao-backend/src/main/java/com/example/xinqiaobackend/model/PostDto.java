package com.example.xinqiaobackend.model;

import java.util.List;

/**
 * 主题交流区 · 帖子数据模型
 */
public class PostDto {
    private String id;
    private String author;
    private boolean anonymous;
    private String time; // 直接传递友好时间文案，前端可后续改为时间戳
    private String title;
    private String content;
    private List<String> tags;
    private int imageCount;
    private Integer voiceDurationSec;

    public PostDto() {}

    public PostDto(String id, String author, boolean anonymous, String time, String title, String content,
                   List<String> tags, int imageCount, Integer voiceDurationSec) {
        this.id = id;
        this.author = author;
        this.anonymous = anonymous;
        this.time = time;
        this.title = title;
        this.content = content;
        this.tags = tags;
        this.imageCount = imageCount;
        this.voiceDurationSec = voiceDurationSec;
    }

    public String getId() { return id; }
    public String getAuthor() { return author; }
    public boolean isAnonymous() { return anonymous; }
    public String getTime() { return time; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public List<String> getTags() { return tags; }
    public int getImageCount() { return imageCount; }
    public Integer getVoiceDurationSec() { return voiceDurationSec; }
}

