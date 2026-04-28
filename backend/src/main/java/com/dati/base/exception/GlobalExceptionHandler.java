package com.dati.base.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.text.MessageFormat;
import java.util.stream.Collectors;

/**
 * Global exception handler that converts all exceptions into a unified {@link ErrorResponse}
 * with appropriate HTTP status codes.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DatiException.class)
    public ResponseEntity<ErrorResponse> handleDatiException(DatiException e) {
        ErrorResponse response = e.getArgs().length > 0
                ? ErrorResponse.of(e.getCode(), e.getArgs())
                : ErrorResponse.ofResolved(e.getCode(), e.getMessage());
        return ResponseEntity.status(e.getStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        String fieldErrors = e.getBindingResult().getFieldErrors().stream()
            .map(err -> MessageFormat.format("Field {0} has invalid value: {1}", err.getField(), err.getDefaultMessage()))
            .collect(Collectors.joining("; "));

        ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_PARAMETER, fieldErrors);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        log.error("Unexpected error occurred", e);
        ErrorResponse response = ErrorResponse.of(ErrorCode.INTERNAL_ERROR);
        return ResponseEntity.status(500).body(response);
    }
}
