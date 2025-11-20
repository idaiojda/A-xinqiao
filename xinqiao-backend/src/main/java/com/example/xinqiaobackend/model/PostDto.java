package com.example.xinqiaobackend.model;

import java.util.List;

/**
 * 主题交流区 · 帖子数据模型
 */
public class PostDto {
    private String id;
    private String author;
    private String authorNickname;
    private String authorAvatar;
    private boolean anonymous;
    private String time; // 直接传递友好时间文案，前端可后续改为时间戳
    private String title;
    private String content;
    private List<String> tags;
    private List<String> images;
    private Integer voiceDurationSec;
    private long createdAtMillis;

    public PostDto() {}

    public PostDto(String id, String author, String authorNickname, String authorAvatar, boolean anonymous, String time, String title, String content,
                   List<String> tags, List<String> images, Integer voiceDurationSec) {
        this.id = id;
        this.author = author;
        this.authorNickname = authorNickname;
        this.authorAvatar = authorAvatar;
        this.anonymous = anonymous;
        this.time = time;
        this.title = title;
        this.content = content;
        this.tags = tags;
        this.images = images;
        this.voiceDurationSec = voiceDurationSec;
        this.createdAtMillis = System.currentTimeMillis();
    }

    public PostDto(String id, String author, String authorNickname, String authorAvatar, boolean anonymous, String time, String title, String content,
                   List<String> tags, List<String> images, Integer voiceDurationSec, long createdAtMillis) {
        this.id = id;
        this.author = author;
        this.authorNickname = authorNickname;
        this.authorAvatar = authorAvatar;
        this.anonymous = anonymous;
        this.time = time;
        this.title = title;
        this.content = content;
        this.tags = tags;
        this.images = images;
        this.voiceDurationSec = voiceDurationSec;
        this.createdAtMillis = createdAtMillis;
    }

    public String getId() { return id; }
    public String getAuthor() { return author; }
    public String getAuthorNickname() { return authorNickname; }
    public String getAuthorAvatar() { return authorAvatar; }
    public boolean isAnonymous() { return anonymous; }
    public String getTime() { return time; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public List<String> getTags() { return tags; }
    public List<String> getImages() { return images; }
    public Integer getVoiceDurationSec() { return voiceDurationSec; }
    public long getCreatedAtMillis() { return createdAtMillis; }
}

