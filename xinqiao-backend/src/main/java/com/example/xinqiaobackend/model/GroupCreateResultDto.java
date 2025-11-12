package com.example.xinqiaobackend.model;

public class GroupCreateResultDto {
    private boolean ok;
    private String message;

    public GroupCreateResultDto(boolean ok, String message) {
        this.ok = ok;
        this.message = message;
    }

    public boolean isOk() { return ok; }
    public String getMessage() { return message; }
}

