package com.dati.base.exception;

import lombok.Getter;

import java.text.MessageFormat;

public class DatiException extends RuntimeException {

    private final ErrorCode errorCode;

    @Getter
    private final Object[] args;

    public DatiException(ErrorCode errorCode, Object... args) {
        super(MessageFormat.format(errorCode.getTemplate(), args));
        this.errorCode = errorCode;
        this.args = args;
    }

    public DatiException(String message) {
        super(message);
        this.errorCode = ErrorCode.INTERNAL_ERROR;
        this.args = new Object[0];
    }


    public DatiException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = ErrorCode.INTERNAL_ERROR;
        this.args = new Object[0];
    }

    public int getStatus() {
        return errorCode.getStatus();
    }

    public ErrorCode getCode() {
        return errorCode;
    }

}
