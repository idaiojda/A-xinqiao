package com.example.xinqiaobackend.model;

public class HealthDto {
    private boolean ok;
    private String message;

    public HealthDto() {}
    public HealthDto(boolean ok, String message) {
        this.ok = ok; this.message = message;
    }
    public boolean isOk() { return ok; }
    public void setOk(boolean ok) { this.ok = ok; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}

