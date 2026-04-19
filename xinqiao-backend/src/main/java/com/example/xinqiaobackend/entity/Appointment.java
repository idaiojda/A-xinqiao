package com.example.xinqiaobackend.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "appointments")
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String userUsername;

    @Column(nullable = false, length = 64)
    private String counselorUsername;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AppointmentStatus status = AppointmentStatus.PENDING;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "mode", length = 16)
    private String mode;

    @Column(name = "remark", length = 512)
    private String remark;

    @Column(name = "price")
    private Double price;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserUsername() { return userUsername; }
    public void setUserUsername(String userUsername) { this.userUsername = userUsername; }
    public String getCounselorUsername() { return counselorUsername; }
    public void setCounselorUsername(String counselorUsername) { this.counselorUsername = counselorUsername; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public AppointmentStatus getStatus() { return status; }
    public void setStatus(AppointmentStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
}