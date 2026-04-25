package com.linkflow.api.common.exception;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.linkflow.api.common.error.ApiException;
import com.linkflow.api.common.response.ApiError;
import com.linkflow.api.common.response.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException ex) {
        ApiError error = new ApiError(ex.getCode(), ex.getMessage(), ex.getDetails());
        return ResponseEntity.status(ex.getStatus()).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NoSuchElementException ex) {
        ApiError error = new ApiError("RESOURCE_NOT_FOUND", ex.getMessage(), Map.of());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> details = new LinkedHashMap<>();
        Object target = ex.getBindingResult().getTarget();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            details.put(resolveFieldName(error, target), error.getDefaultMessage());
        }

        ApiError error = new ApiError("VALIDATION_ERROR", "Request validation failed.", details);
        return ResponseEntity.badRequest().body(ApiResponse.failure(error));
    }

    private String resolveFieldName(FieldError error, Object target) {
        Class<?> targetType = target == null ? null : target.getClass();
        if (targetType == null) {
            return error.getField();
        }

        if (targetType.isRecord()) {
            for (RecordComponent component : targetType.getRecordComponents()) {
                if (!component.getName().equals(error.getField())) {
                    continue;
                }
                JsonProperty property = component.getAnnotation(JsonProperty.class);
                if (property != null && !property.value().isBlank()) {
                    return property.value();
                }
            }
        }

        try {
            Field field = targetType.getDeclaredField(error.getField());
            JsonProperty property = field.getAnnotation(JsonProperty.class);
            if (property != null && !property.value().isBlank()) {
                return property.value();
            }
        } catch (NoSuchFieldException ignored) {
        }

        return error.getField();
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, Object> details = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String path = violation.getPropertyPath().toString();
            int separator = path.lastIndexOf('.');
            String field = separator >= 0 ? path.substring(separator + 1) : path;
            details.put(field, violation.getMessage());
        }

        ApiError error = new ApiError("VALIDATION_ERROR", "Request validation failed.", details);
        return ResponseEntity.badRequest().body(ApiResponse.failure(error));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleHandlerMethodValidation(HandlerMethodValidationException ex) {
        Map<String, Object> details = new LinkedHashMap<>();
        ex.getParameterValidationResults().forEach(result -> {
            String field = result.getMethodParameter().getParameterName();
            result.getResolvableErrors().forEach(error -> {
                if (field != null && !field.isBlank() && !details.containsKey(field)) {
                    details.put(field, error.getDefaultMessage());
                }
            });
        });

        ApiError error = new ApiError("VALIDATION_ERROR", "Request validation failed.", details);
        return ResponseEntity.badRequest().body(ApiResponse.failure(error));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException ex) {
        ApiError error = new ApiError("INTERNAL_ERROR", ex.getMessage(), Map.of());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.failure(error));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        ApiError error = new ApiError("INTERNAL_ERROR", "Unexpected server error.", Map.of());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.failure(error));
    }
}
