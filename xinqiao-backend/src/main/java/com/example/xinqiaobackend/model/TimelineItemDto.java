package com.example.xinqiaobackend.model;

public class TimelineItemDto {
    private String type; // checkin/share/badge
    private String text;
    private long timestamp;

    public TimelineItemDto() {}
    public TimelineItemDto(String type, String text, long timestamp) {
        this.type = type; this.text = text; this.timestamp = timestamp;
    }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}

