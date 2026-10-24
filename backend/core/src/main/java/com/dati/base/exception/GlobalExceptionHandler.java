package com.dati.base.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Global exception handler converting all exceptions into a unified {@link ErrorResponse}
 * with appropriate HTTP status codes.
 * Standard Spring MVC exceptions are mapped to 4xx status codes by the parent
 * {@link ResponseEntityExceptionHandler}; their response bodies are normalized to
 * {@link ErrorResponse} in {@link #handleExceptionInternal}. {@link DatiException},
 * {@link BindException} and unexpected exceptions are handled directly.
 * <p>
 * Error bodies always preset {@code application/json}: error responses (401/403 in
 * particular) must be machine-readable even when the request only accepts a stream
 * (e.g. an unauthenticated MCP {@code text/event-stream} client).
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private final ObjectMapper objectMapper;

    public GlobalExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @ExceptionHandler(DatiException.class)
    public ResponseEntity<ErrorResponse> handleDatiException(DatiException e) {
        ErrorResponse response = e.getArgs().length > 0
                ? ErrorResponse.of(e.getCode(), e.getArgs())
                : ErrorResponse.ofResolved(e.getCode(), e.getMessage());
        return ResponseEntity.status(e.getStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(BindException e) {
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ErrorResponse.of(ErrorCode.INVALID_PARAMETER, formatFieldErrors(e.getBindingResult())));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, @NonNull HttpHeaders headers,
                                                             @NonNull HttpStatusCode status, @NonNull WebRequest request) {
        String message;
        if (ex instanceof HttpMessageNotReadableException notReadable) {
            message = deserializationMessage(notReadable);
        } else {
            message = switch (ex) {
                case MethodArgumentNotValidException e -> formatFieldErrors(e.getBindingResult());
                case HandlerMethodValidationException e -> formatParameterErrors(e);
                default -> extractMessage(body, ex);
            };
        }
        return ResponseEntity.status(status).headers(headers)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ErrorResponse.of(codeFor(status), message));
    }

    /** Field-level details for @Valid @RequestBody objects, e.g. "jdbc_url: must not be blank". */
    private String formatFieldErrors(BindingResult bindingResult) {
        return bindingResult.getFieldErrors().stream()
                .map(err -> jsonFieldName(err.getField()) + ": "
                        + (err.getDefaultMessage() != null ? err.getDefaultMessage() : err.toString()))
                .collect(Collectors.joining("; "));
    }

    /** Field-level details for container/method parameter validation, e.g. "tables[0]: must not be blank". */
    private String formatParameterErrors(HandlerMethodValidationException ex) {
        return ex.getParameterValidationResults().stream()
                .flatMap(result -> {
                    String paramName = result.getMethodParameter().getParameterName();
                    String path = (paramName != null ? jsonFieldName(paramName) : "parameter")
                            + (result.getContainerIndex() != null ? "[" + result.getContainerIndex() + "]" : "");
                    return result.getResolvableErrors().stream()
                            .map(err -> path + ": "
                                    + (err.getDefaultMessage() != null ? err.getDefaultMessage() : err.toString()));
                })
                .collect(Collectors.joining("; "));
    }

    /**
     * Reports the failing field and the received value space instead of Jackson's raw
     * "Failed to read request". Enum mismatches list the accepted constants so callers can
     * correct the request without guessing.
     */
    private static String deserializationMessage(HttpMessageNotReadableException ex) {
        Throwable cause = ex.getMostSpecificCause();
        if (cause instanceof InvalidFormatException invalidFormat) {
            String field = jsonPath(invalidFormat);
            Class<?> targetType = invalidFormat.getTargetType();
            if (targetType != null && targetType.isEnum()) {
                String allowed = Arrays.stream(targetType.getEnumConstants())
                        .map(String::valueOf)
                        .collect(Collectors.joining(", "));
                return withField(field, "must be one of [" + allowed + "]");
            }
            return withField(field, concise(invalidFormat.getOriginalMessage()));
        }
        if (cause instanceof MismatchedInputException mismatched) {
            return withField(jsonPath(mismatched), concise(mismatched.getOriginalMessage()));
        }
        return concise(cause.getMessage());
    }

    private static String jsonPath(JsonMappingException ex) {
        return ex.getPath().stream()
                .map(JsonMappingException.Reference::getFieldName)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("."));
    }

    private static String withField(String field, String message) {
        String detail = concise(message);
        return field == null || field.isBlank() ? detail : field + ": " + detail;
    }

    private static String concise(String message) {
        if (message == null || message.isBlank()) {
            return "invalid request body";
        }
        String value = message.replaceAll("\\s+", " ").trim();
        int sourceIndex = value.indexOf(" at [Source:");
        if (sourceIndex > 0) {
            value = value.substring(0, sourceIndex).trim();
        }
        return value.length() <= 300 ? value : value.substring(0, 300) + "...";
    }

    /** Translates a Java property path into the JSON field name configured for the API. */
    private String jsonFieldName(String path) {
        if (path == null || path.isBlank()) {
            return path;
        }
        PropertyNamingStrategy strategy = objectMapper.getSerializationConfig().getPropertyNamingStrategy();
        if (strategy == null) {
            return path;
        }
        return Arrays.stream(path.split("\\."))
                .map(segment -> translateSegment(segment, strategy))
                .collect(Collectors.joining("."));
    }

    private static String translateSegment(String segment, PropertyNamingStrategy strategy) {
        int bracket = segment.indexOf('[');
        String field = bracket < 0 ? segment : segment.substring(0, bracket);
        String suffix = bracket < 0 ? "" : segment.substring(bracket);
        String translated = strategy.nameForField(null, null, field);
        return (translated != null ? translated : field) + suffix;
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
        return ResponseEntity.status(500)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
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
