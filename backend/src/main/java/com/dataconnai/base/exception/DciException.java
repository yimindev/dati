package com.dataconnai.base.exception;

public class DciException extends RuntimeException {

    private final String message;

    public DciException(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }

}
