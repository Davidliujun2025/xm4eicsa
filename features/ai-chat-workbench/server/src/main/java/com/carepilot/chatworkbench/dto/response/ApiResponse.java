package com.carepilot.chatworkbench.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private Integer code;
    private String message;
    private T data;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .code(200)
                .message("success")
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .code(200)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> error(Integer code, String message) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .data(null)
                .build();
    }

    public static <T> ApiResponse<T> conflict(String message) {
        return ApiResponse.<T>builder()
                .code(409)
                .message(message)
                .data(null)
                .build();
    }

    public static <T> ApiResponse<T> forbidden(String message) {
        return ApiResponse.<T>builder()
                .code(403)
                .message(message)
                .data(null)
                .build();
    }

    public static <T> ApiResponse<T> timeout(String message) {
        return ApiResponse.<T>builder()
                .code(504)
                .message(message)
                .data(null)
                .build();
    }
}