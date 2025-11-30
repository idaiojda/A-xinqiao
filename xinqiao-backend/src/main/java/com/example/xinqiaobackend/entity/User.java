package com.example.xinqiaobackend.entity;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_info")
public class User {
    @Id
    @Column(name = "user_id")
    private Long id;

    @Column(unique = true, nullable = false, length = 64)
    private String username;

    @Column(nullable = false, length = 128)
    private String password;

    @Column(name = "roles", length = 256)
    private String rolesCsv;

    @Column(name = "review_status", length = 16)
    private String reviewStatus = "PENDING";

    

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public List<String> getRoles() {
        if (rolesCsv == null || rolesCsv.trim().isEmpty()) return new ArrayList<>();
        String[] arr = rolesCsv.split(",");
        List<String> list = new ArrayList<>();
        for (String s : arr) { String t = s.trim(); if (!t.isEmpty()) list.add(t); }
        return list;
    }
    public void setRoles(List<String> roles) {
        if (roles == null || roles.isEmpty()) { this.rolesCsv = null; return; }
        this.rolesCsv = String.join(",", roles);
    }
    public String getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(String reviewStatus) { this.reviewStatus = reviewStatus; }
    
}

