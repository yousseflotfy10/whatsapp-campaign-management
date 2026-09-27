package com.wcm.campaign.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException exception) {
        Map<String, Object> errors = new LinkedHashMap<>();

        errors.put("status", HttpStatus.BAD_REQUEST.value());
        errors.put("error", "Validation failed");

        Map<String, String> fields = new LinkedHashMap<>();

        exception.getBindingResult().getFieldErrors()
                .forEach(error -> fields.put(error.getField(), error.getDefaultMessage()));

        errors.put("fields", fields);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }


    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException exception) {

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Bad request");
        response.put("message", exception.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }


    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException exception) {

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("status", HttpStatus.BAD_REQUEST.value());

        response.put("error", "Request failed");

        response.put("message", exception.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}