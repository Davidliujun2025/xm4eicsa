package com.acme.aicslogin.api;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(ApiResponse.error(exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException exception) {
        FieldError error = exception.getBindingResult().getFieldErrors().stream()
                .sorted((left, right) -> left.getField().compareTo(right.getField()))
                .findFirst()
                .orElse(null);
        if (error != null && "account".equals(error.getField())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("ACCOUNT_REQUIRED", "请输入账号"));
        }
        if (error != null && "password".equals(error.getField())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("PASSWORD_REQUIRED", "请输入密码"));
        }
        return ResponseEntity.badRequest().body(ApiResponse.error("INVALID_REQUEST", "请求参数不正确"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiResponse<Void>> handleUnreadableBody(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(ApiResponse.error("INVALID_REQUEST", "请求参数不正确"));
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<ApiResponse<Void>> handleDataAccess(DataAccessException exception, HttpServletRequest request) {
        log.error("Database access error while processing {} {}", request.getMethod(), request.getRequestURI(), exception);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error("DATABASE_UNAVAILABLE", "服务暂不可用，请稍后重试"));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("Unexpected error while processing {} {}", request.getMethod(), request.getRequestURI(), exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("INTERNAL_ERROR", "系统繁忙，请稍后重试"));
    }
}
