package com.carepilot.agentaccount.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException exception) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();

        exception.getBindingResult()
                .getFieldErrors()
                .forEach(error -> fieldErrors.putIfAbsent(
                        error.getField(),
                        error.getDefaultMessage()
                ));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("fieldErrors", fieldErrors);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", "VALIDATION_ERROR");
        response.put("message", "参数校验失败");
        response.put("data", data);

        return ResponseEntity.badRequest().body(response);
    }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
            IllegalArgumentException exception) {

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", "VALIDATION_ERROR");
        response.put("message", exception.getMessage());
        response.put("data", null);

        return ResponseEntity.badRequest().body(response);
   }
}