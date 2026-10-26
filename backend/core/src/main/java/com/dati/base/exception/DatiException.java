package com.dati.base.exception;

import lombok.Getter;

public class DatiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final boolean customMessage;

    @Getter
    private final Object[] args;

    public DatiException(ErrorCode errorCode, Object... args) {
        super(ErrorMessages.format(errorCode.getTemplate(), args));
        this.errorCode = errorCode;
        this.args = args;
        this.customMessage = false;
    }

    public DatiException(String message) {
        super(message);
        this.errorCode = ErrorCode.INTERNAL_ERROR;
        this.args = new Object[0];
        this.customMessage = true;
    }


    public DatiException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = ErrorCode.INTERNAL_ERROR;
        this.args = new Object[0];
        this.customMessage = true;
    }

    public int getStatus() {
        return errorCode.getStatus();
    }

    public ErrorCode getCode() {
        return errorCode;
    }

    /** True when the message was supplied directly instead of derived from an ErrorCode template. */
    public boolean isCustomMessage() {
        return customMessage;
    }

}
