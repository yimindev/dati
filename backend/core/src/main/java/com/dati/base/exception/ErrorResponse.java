package com.dati.base.exception;

import lombok.Getter;

import java.text.MessageFormat;

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
        return new ErrorResponse(errorCode.getCode(), MessageFormat.format(errorCode.getTemplate(), new Object[0]));
    }

    public static ErrorResponse of(ErrorCode errorCode, Object... args) {
        return new ErrorResponse(errorCode.getCode(), MessageFormat.format(errorCode.getTemplate(), args));
    }

    public static ErrorResponse ofResolved(ErrorCode errorCode, String resolvedMessage) {
        return new ErrorResponse(errorCode.getCode(), resolvedMessage);
    }

}
