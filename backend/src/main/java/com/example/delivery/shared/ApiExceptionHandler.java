package com.example.delivery.shared;

import com.example.delivery.delivery.DeliveryNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(ApiExceptionHandler.class);

    public record ApiError(String message, Map<String, String> errors) {}

    @ExceptionHandler(DeliveryNotFoundException.class)
    public ResponseEntity<ApiError> handleDeliveryNotFound(DeliveryNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError(exception.getMessage(), Map.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationFailure(
            MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception
                .getBindingResult()
                .getFieldErrors()
                .forEach(
                        fieldError ->
                                fieldErrors.putIfAbsent(
                                        fieldError.getField(), fieldError.getDefaultMessage()));
        return ResponseEntity.badRequest()
                .body(new ApiError("Periksa isian formulir", fieldErrors));
    }

    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiError> handleMalformedRequest(Exception exception) {
        return ResponseEntity.badRequest()
                .body(
                        new ApiError(
                                "Format permintaan, ID, zona, atau status tidak valid", Map.of()));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiError> handleDatabaseFailure(DataAccessException exception) {
        logger.error("Database operation failed ({})", exception.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(
                        new ApiError(
                                "Database sedang tidak tersedia. Coba kembali nanti.", Map.of()));
    }
}
