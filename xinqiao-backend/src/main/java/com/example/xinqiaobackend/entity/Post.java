package com.example.xinqiaobackend.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "posts")
public class Post {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Lob
    @Column(nullable = false)
    private String content;

    // 存储为 JSON 字符串，如 ["标签1","标签2"]
    @Column(name = "tags_json", columnDefinition = "TEXT")
    private String tagsJson;

    // 存储为 JSON 字符串，如 ["/uploads/img1.jpg"]
    @Column(name = "images_json", columnDefinition = "TEXT")
    private String imagesJson;

    @Transient
    private List<String> tags;

    @Transient
    private List<String> images;

    @Column(nullable = false)
    private boolean anonymous;

    @Column(length = 128)
    private String authorName;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private String authorAvatar;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(length = 64)
    private String category;

    @Column(length = 16)
    private String reviewStatus = "PENDING";

    @PrePersist
    @javax.persistence.PreUpdate
    public void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        // 确保 JSON 字段与 transient 字段同步
        if (tags != null) tagsJson = toJson(tags);
        if (images != null) imagesJson = toJson(images);
    }

    @javax.persistence.PostLoad
    public void postLoad() {
        tags = parseJson(tagsJson);
        images = parseJson(imagesJson);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public List<String> getTags() {
        if (tags == null) tags = parseJson(tagsJson);
        return tags;
    }
    public void setTags(List<String> tags) {
        this.tags = tags;
        this.tagsJson = toJson(tags);
    }

    public List<String> getImages() {
        if (images == null) images = parseJson(imagesJson);
        return images;
    }
    public void setImages(List<String> images) {
        this.images = images;
        this.imagesJson = toJson(images);
    }

    private List<String> parseJson(String json) {
        if (json == null || json.trim().isEmpty()) return new ArrayList<>();
        try {
            return MAPPER.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String toJson(List<String> list) {
        if (list == null || list.isEmpty()) return null;
        try {
            return MAPPER.writeValueAsString(list);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isAnonymous() { return anonymous; }
    public void setAnonymous(boolean anonymous) { this.anonymous = anonymous; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public String getAuthorAvatar() { return authorAvatar; }
    public void setAuthorAvatar(String authorAvatar) { this.authorAvatar = authorAvatar; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }
}
