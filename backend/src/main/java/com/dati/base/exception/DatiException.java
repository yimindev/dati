package com.dati.base.exception;

public class DatiException extends RuntimeException {

    private final String message;

    public DatiException(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }

}
