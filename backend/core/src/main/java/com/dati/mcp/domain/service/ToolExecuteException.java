package com.dati.mcp.domain.service;

import com.dati.mcp.domain.model.ToolError;

public class ToolExecuteException extends RuntimeException {

    private final ToolError error;

    public ToolExecuteException(ToolError error, Object... args) {
        super(error.format(args));
        this.error = error;
    }

    public ToolError getToolError() {
        return error;
    }

    public String getErrorCategory() {
        return error.getCategory();
    }
}
