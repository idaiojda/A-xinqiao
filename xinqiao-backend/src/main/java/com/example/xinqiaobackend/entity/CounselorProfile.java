package com.example.xinqiaobackend.entity;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "counselor_profiles")
public class CounselorProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "username", nullable = false, length = 64)
    private String username;

    @Column(name = "title", length = 64)
    private String title;

    @Column(name = "default_mode", length = 16)
    private String defaultMode = "text";

    @Column(length = 64)
    private String city;
    
    @Column(name = "display_name", length = 64)
    private String displayName;
    
    @Column(name = "brief_intro", length = 200)
    private String briefIntro;
    
    @Column(name = "education", length = 32)
    private String education;
    
    @Column(name = "work_years", length = 16)
    private String workYears;
    
    @Column(name = "detailed_intro", length = 2000)
    private String detailedIntro;
    
    @Lob
    @Column(name = "avatar_base64", columnDefinition = "LONGTEXT")
    private String avatarBase64;
    
    @Column(name = "status", length = 16)
    private String status = "pending";

    @Column
    private Double price;

    @Column(name = "price_text")
    private Double priceText;

    @Column(name = "price_voice")
    private Double priceVoice;

    @Column(name = "price_video")
    private Double priceVideo;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "counselor_profile_tags", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "tag", length = 32)
    private List<String> tags = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDefaultMode() { return defaultMode; }
    public void setDefaultMode(String defaultMode) { this.defaultMode = defaultMode; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getBriefIntro() { return briefIntro; }
    public void setBriefIntro(String briefIntro) { this.briefIntro = briefIntro; }
    public String getEducation() { return education; }
    public void setEducation(String education) { this.education = education; }
    public String getWorkYears() { return workYears; }
    public void setWorkYears(String workYears) { this.workYears = workYears; }
    public String getDetailedIntro() { return detailedIntro; }
    public void setDetailedIntro(String detailedIntro) { this.detailedIntro = detailedIntro; }
    public String getAvatarBase64() { return avatarBase64; }
    public void setAvatarBase64(String avatarBase64) { this.avatarBase64 = avatarBase64; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public Double getPriceText() { return priceText; }
    public void setPriceText(Double priceText) { this.priceText = priceText; }
    public Double getPriceVoice() { return priceVoice; }
    public void setPriceVoice(Double priceVoice) { this.priceVoice = priceVoice; }
    public Double getPriceVideo() { return priceVideo; }
    public void setPriceVideo(Double priceVideo) { this.priceVideo = priceVideo; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
}