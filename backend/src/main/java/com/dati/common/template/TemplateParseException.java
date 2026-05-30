package com.dati.common.template;

/**
 * Thrown when template text has a syntax error that prevents parsing.
 */
public class TemplateParseException extends RuntimeException {
    public TemplateParseException(String message) { super(message); }
    public TemplateParseException(String message, Throwable cause) { super(message, cause); }
}
