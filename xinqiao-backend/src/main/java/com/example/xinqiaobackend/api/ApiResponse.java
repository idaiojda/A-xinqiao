package com.example.xinqiaobackend.api;

public class ApiResponse<T> {
    private boolean ok;
    private int code;
    private String message;
    private T data;

    public ApiResponse() {}
    public ApiResponse(boolean ok, int code, String message, T data) { this.ok = ok; this.code = code; this.message = message; this.data = data; }
    public static <T> ApiResponse<T> success(T data) { return new ApiResponse<>(true, 0, "", data); }
    public static <T> ApiResponse<T> successMessage(String message) { return new ApiResponse<>(true, 0, message, null); }
    public static <T> ApiResponse<T> error(int code, String message) { return new ApiResponse<>(false, code, message, null); }
    public boolean isOk() { return ok; }
    public void setOk(boolean ok) { this.ok = ok; }
    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}