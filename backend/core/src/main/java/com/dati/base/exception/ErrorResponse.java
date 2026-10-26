package com.dati.base.exception;

import lombok.Getter;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

@Getter
public class ErrorResponse {

    private final String code;
    private final String message;
    private final long timestamp;

    private ErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    public static ErrorResponse of(ErrorCode errorCode) {
        return of(errorCode, new Object[0]);
    }

    public static ErrorResponse of(ErrorCode errorCode, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();
        return new ErrorResponse(errorCode.getCode(), ErrorMessages.formatLocalized(errorCode, args, locale));
    }

    public static ErrorResponse ofResolved(ErrorCode errorCode, String resolvedMessage) {
        return new ErrorResponse(errorCode.getCode(), resolvedMessage);
    }

}
