package com.example.xinqiaobackend.model;

public class NewCommentRequest {
    private String author;
    private String text;

    public NewCommentRequest() {}
    public NewCommentRequest(String author, String text) {
        this.author = author; this.text = text;
    }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}

