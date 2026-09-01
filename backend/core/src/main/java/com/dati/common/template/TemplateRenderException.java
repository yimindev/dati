package com.dati.common.template;

/**
 * Thrown when rendering encounters a structural issue in the AST
 * (e.g., unrecognized Node type). NOT thrown for missing parameter
 * values — those are handled leniently.
 */
public class TemplateRenderException extends RuntimeException {
    public TemplateRenderException(String message) { super(message); }
    public TemplateRenderException(String message, Throwable cause) { super(message, cause); }
}
