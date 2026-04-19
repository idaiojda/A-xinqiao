package com.example.xinqiaobackend.config;

import com.example.xinqiaobackend.api.ApiResponse;
import com.example.xinqiaobackend.api.ErrorCode;
import org.apache.catalina.connector.ClientAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {
        log.error("Validation error", ex);
        return ResponseEntity.badRequest().body(ApiResponse.error(ErrorCode.BAD_REQUEST, "参数错误"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.error("Data integrity violation", ex);
        String message = "数据完整性错误";
        if (ex.getMessage() != null) {
            if (ex.getMessage().contains("user_id")) {
                message = "用户ID不能为空";
            } else if (ex.getMessage().contains("username")) {
                message = "用户名不能为空";
            }
        }
        return ResponseEntity.status(500).body(ApiResponse.error(ErrorCode.SERVER_ERROR, message));
    }

    @ExceptionHandler(ClientAbortException.class)
    public void handleClientAbort(ClientAbortException ex, javax.servlet.http.HttpServletRequest request) {
        // 客户端主动断开连接，这是正常行为，只记录 DEBUG 级别日志
        log.debug("Client aborted connection: {} {}", request.getMethod(), request.getRequestURI());
        // 不返回响应，因为客户端已经断开连接
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleAny(Exception ex, javax.servlet.http.HttpServletRequest request) {
        // 检查是否是客户端中止异常（包括嵌套的情况）
        Throwable cause = ex;
        while (cause != null) {
            if (cause instanceof ClientAbortException || 
                cause.getClass().getName().contains("ClientAbortException")) {
                log.debug("Client aborted connection: {} {}", request.getMethod(), request.getRequestURI());
                return null; // 客户端已断开，无需返回响应
            }
            cause = cause.getCause();
        }
        
        log.error("Unhandled exception: " + ex.getClass().getName() + " - " + ex.getMessage() + " | Request: " + request.getMethod() + " " + request.getRequestURI(), ex);
        String detailMessage = ex.getMessage() != null ? ex.getMessage() : "未知错误";
        return ResponseEntity.status(500).body(ApiResponse.error(ErrorCode.SERVER_ERROR, "服务器错误: " + detailMessage));
    }
}
