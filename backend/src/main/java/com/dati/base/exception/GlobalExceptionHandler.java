package com.dati.base.exception;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler converting all exceptions into a unified {@link ErrorResponse}
 * with appropriate HTTP status codes.
 * Standard Spring MVC exceptions are mapped to 4xx status codes by the parent
 * {@link ResponseEntityExceptionHandler}; their response bodies are normalized to
 * {@link ErrorResponse} in {@link #handleExceptionInternal}. {@link DatiException},
 * {@link BindException} and unexpected exceptions are handled directly.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(DatiException.class)
    public ResponseEntity<ErrorResponse> handleDatiException(DatiException e) {
        ErrorResponse response = e.getArgs().length > 0
                ? ErrorResponse.of(e.getCode(), e.getArgs())
                : ErrorResponse.ofResolved(e.getCode(), e.getMessage());
        return ResponseEntity.status(e.getStatus()).body(response);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(BindException e) {
        return ResponseEntity.badRequest().body(ErrorResponse.of(ErrorCode.INVALID_PARAMETER, formatFieldErrors(e.getBindingResult())));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, @NonNull HttpHeaders headers,
                                                             @NonNull HttpStatusCode status, @NonNull WebRequest request) {
        String message = switch (ex) {
            case MethodArgumentNotValidException e -> formatFieldErrors(e.getBindingResult());
            case HandlerMethodValidationException e -> formatParameterErrors(e);
            default -> extractMessage(body, ex);
        };
        return ResponseEntity.status(status).headers(headers)
                .body(ErrorResponse.of(codeFor(status), message));
    }

    /** Field-level details for @Valid @RequestBody objects, e.g. "name: must not be blank". */
    private static String formatFieldErrors(BindingResult bindingResult) {
        return bindingResult.getFieldErrors().stream()
                .map(err -> err.getField() + ": "
                        + (err.getDefaultMessage() != null ? err.getDefaultMessage() : err.toString()))
                .collect(Collectors.joining("; "));
    }

    /** Field-level details for container/method parameter validation, e.g. "tables[0]: must not be blank". */
    private static String formatParameterErrors(HandlerMethodValidationException ex) {
        return ex.getParameterValidationResults().stream()
                .flatMap(result -> {
                    String paramName = result.getMethodParameter().getParameterName();
                    String path = (paramName != null ? paramName : "parameter")
                            + (result.getContainerIndex() != null ? "[" + result.getContainerIndex() + "]" : "");
                    return result.getResolvableErrors().stream()
                            .map(err -> path + ": "
                                    + (err.getDefaultMessage() != null ? err.getDefaultMessage() : err.toString()));
                })
                .collect(Collectors.joining("; "));
    }

    private static String extractMessage(Object body, Exception ex) {
        if (body instanceof ProblemDetail pd) {
            // Validation failures carry field-level details in the "errors" extension.
            Object errors = pd.getProperties() != null ? pd.getProperties().get("errors") : null;
            if (errors instanceof List<?> list && !list.isEmpty()) {
                return list.stream()
                        .map(GlobalExceptionHandler::formatError)
                        .collect(Collectors.joining("; "));
            }
            if (pd.getDetail() != null && !pd.getDetail().isBlank()) {
                return pd.getDetail();
            }
            if (pd.getTitle() != null) {
                return pd.getTitle();
            }
        }
        if (body != null) {
            return body.toString();
        }
        return ex.getMessage() != null && !ex.getMessage().isBlank()
                ? ex.getMessage()
                : ex.getClass().getSimpleName();
    }

    private static String formatError(Object error) {
        if (error instanceof Map<?, ?> m && m.get("propertyPath") != null) {
            return m.get("propertyPath") + ": " + m.get("message");
        }
        return String.valueOf(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        log.error("Unexpected error occurred", e);
        ErrorResponse response = ErrorResponse.of(ErrorCode.INTERNAL_ERROR);
        return ResponseEntity.status(500).body(response);
    }

    /** Map an HTTP status to the closest generic error code. */
    private static ErrorCode codeFor(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> ErrorCode.INVALID_PARAMETER;
            case 404 -> ErrorCode.NOT_FOUND;
            case 405 -> ErrorCode.METHOD_NOT_ALLOWED;
            default -> status.is4xxClientError() ? ErrorCode.INVALID_PARAMETER : ErrorCode.INTERNAL_ERROR;
        };
    }
}
