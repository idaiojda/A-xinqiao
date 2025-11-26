package com.example.xinqiao.model;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Appointment model for counselor appointment management
 */
public class Appointment implements Serializable {
    
    private String id;
    private String userId;
    private String userName;
    private String userAvatar;
    private Date appointmentTime;
    private String consultationType; // video, voice, text
    private String status; // pending, confirmed, completed
    private String notes;
    private Date createdAt;
    
    public Appointment() {
        this.createdAt = new Date();
    }
    
    public Appointment(String id, String userName, String userAvatar, Date appointmentTime, String consultationType, String status) {
        this.id = id;
        this.userName = userName;
        this.userAvatar = userAvatar;
        this.appointmentTime = appointmentTime;
        this.consultationType = consultationType;
        this.status = status;
        this.createdAt = new Date();
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getUserName() {
        return userName;
    }
    
    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    public String getUserAvatar() {
        return userAvatar;
    }
    
    public void setUserAvatar(String userAvatar) {
        this.userAvatar = userAvatar;
    }
    
    public Date getAppointmentTime() {
        return appointmentTime;
    }
    
    public void setAppointmentTime(Date appointmentTime) {
        this.appointmentTime = appointmentTime;
    }
    
    public String getConsultationType() {
        return consultationType;
    }
    
    public void setConsultationType(String consultationType) {
        this.consultationType = consultationType;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public Date getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    
    /**
     * Get formatted appointment time for display
     */
    public String getFormattedTime() {
        if (appointmentTime == null) return "";
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return dateFormat.format(appointmentTime);
    }
    
    /**
     * Get consultation type display text
     */
    public String getConsultationTypeText() {
        if (consultationType == null) return "";
        
        switch (consultationType) {
            case "video":
                return "视频咨询";
            case "voice":
                return "语音咨询";
            case "text":
                return "文字咨询";
            default:
                return "咨询";
        }
    }
    
    /**
     * Get status display text
     */
    public String getStatusText() {
        if (status == null) return "";
        
        switch (status) {
            case "pending":
                return "待处理";
            case "confirmed":
                return "已确认";
            case "completed":
                return "已完成";
            case "cancelled":
                return "已取消";
            default:
                return status;
        }
    }
    
    /**
     * Check if appointment is upcoming (within next 24 hours)
     */
    public boolean isUpcoming() {
        if (appointmentTime == null) return false;
        
        Date now = new Date();
        long diff = appointmentTime.getTime() - now.getTime();
        long hours = diff / (1000 * 60 * 60);
        
        return hours > 0 && hours <= 24;
    }
    
    /**
     * Check if appointment is overdue
     */
    public boolean isOverdue() {
        if (appointmentTime == null) return false;
        
        Date now = new Date();
        return appointmentTime.before(now) && !status.equals("completed");
    }
}