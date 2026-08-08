package com.dati.base.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(BindException e) {
        String fieldErrors = e.getBindingResult().getFieldErrors().stream()
            .map(err -> MessageFormat.format("Field {0} has invalid value: {1}", err.getField(), err.getDefaultMessage()))
            .collect(Collectors.joining("; "));

        ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_PARAMETER, fieldErrors);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        String fieldErrors = e.getBindingResult().getFieldErrors().stream()
            .map(err -> MessageFormat.format("Field {0} has invalid value: {1}", err.getField(), err.getDefaultMessage()))
            .collect(Collectors.joining("; "));

        ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_PARAMETER, fieldErrors);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException e) {
        ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_PARAMETER,
                "Request body is not readable: " + e.getMostSpecificCause().getMessage());
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_PARAMETER,
                "Parameter " + e.getName() + " has invalid value: " + e.getValue());
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        ErrorResponse response = ErrorResponse.of(ErrorCode.METHOD_NOT_ALLOWED, e.getMethod());
        return ResponseEntity.status(405).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        log.error("Unexpected error occurred", e);
        ErrorResponse response = ErrorResponse.of(ErrorCode.INTERNAL_ERROR);
        return ResponseEntity.status(500).body(response);
    }
}
