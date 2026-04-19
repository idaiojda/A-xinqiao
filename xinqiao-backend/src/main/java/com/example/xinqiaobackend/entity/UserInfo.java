package com.example.xinqiaobackend.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_info")
public class UserInfo {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
    private User user;

    @Column(name = "username", length = 64)
    private String username;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
