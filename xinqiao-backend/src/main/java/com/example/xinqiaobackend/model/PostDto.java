package com.example.xinqiaobackend.model;

import java.io.Serializable;
import java.util.List;

/**
 * 主题交流区 · 帖子数据模型
 */
public class PostDto implements Serializable {
    private static final long serialVersionUID = 1L;
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
    private int likeCount;
    private int commentCount;
    private boolean liked; // 当前用户是否已点赞
    private String reviewStatus; // 审核状态：PENDING, APPROVED, REJECTED

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
        this.likeCount = 0;
        this.commentCount = 0;
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
        this.likeCount = 0;
        this.commentCount = 0;
    }
    
    public PostDto(String id, String author, String authorNickname, String authorAvatar, boolean anonymous, String time, String title, String content,
                   List<String> tags, List<String> images, Integer voiceDurationSec, long createdAtMillis, int likeCount, int commentCount) {
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
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.liked = false;
    }
    
    public PostDto(String id, String author, String authorNickname, String authorAvatar, boolean anonymous, String time, String title, String content,
                   List<String> tags, List<String> images, Integer voiceDurationSec, long createdAtMillis, int likeCount, int commentCount, boolean liked) {
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
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.liked = liked;
        this.reviewStatus = "APPROVED";
    }
    
    public PostDto(String id, String author, String authorNickname, String authorAvatar, boolean anonymous, String time, String title, String content,
                   List<String> tags, List<String> images, Integer voiceDurationSec, long createdAtMillis, int likeCount, int commentCount, boolean liked, String reviewStatus) {
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
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.liked = liked;
        this.reviewStatus = reviewStatus != null ? reviewStatus : "APPROVED";
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
    public int getLikeCount() { return likeCount; }
    public int getCommentCount() { return commentCount; }
    public boolean isLiked() { return liked; }
    public String getReviewStatus() { return reviewStatus; }
    
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }
    public void setLiked(boolean liked) { this.liked = liked; }
    public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }
}

