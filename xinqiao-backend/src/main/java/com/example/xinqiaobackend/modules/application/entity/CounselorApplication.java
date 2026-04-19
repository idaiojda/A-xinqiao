package com.example.xinqiaobackend.modules.application.entity;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "counselor_applications", indexes = {
        @Index(name = "idx_user_status", columnList = "user_id,status")
})
public class CounselorApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "real_name", length = 64)
    private String realName;

    @Column(name = "phone", length = 32)
    private String phone;

    @Column(name = "qualification_type", length = 64)
    private String qualificationType;

    @Column(name = "years")
    private Integer years;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "counselor_application_expertise", joinColumns = @JoinColumn(name = "application_id"))
    @Column(name = "expertise", length = 64)
    private List<String> expertise = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "counselor_application_materials", joinColumns = @JoinColumn(name = "application_id"))
    @Column(name = "material_url", length = 256)
    private List<String> materials = new ArrayList<>();

    @Column(name = "intro", length = 1024)
    private String intro;

    @Column(name = "status", length = 16)
    private String status;

    @Column(name = "rejected_reason", length = 256)
    private String rejectedReason;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getQualificationType() { return qualificationType; }
    public void setQualificationType(String qualificationType) { this.qualificationType = qualificationType; }
    public Integer getYears() { return years; }
    public void setYears(Integer years) { this.years = years; }
    public List<String> getExpertise() { return expertise; }
    public void setExpertise(List<String> expertise) { this.expertise = expertise; }
    public List<String> getMaterials() { return materials; }
    public void setMaterials(List<String> materials) { this.materials = materials; }
    public String getIntro() { return intro; }
    public void setIntro(String intro) { this.intro = intro; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRejectedReason() { return rejectedReason; }
    public void setRejectedReason(String rejectedReason) { this.rejectedReason = rejectedReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
