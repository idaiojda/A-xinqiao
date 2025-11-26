package com.example.xinqiaobackend.model;

public class CommentMessage {
    private String text;
    private Long parentId;

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
}
