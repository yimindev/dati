package com.dati.mcp.domain.model;

import com.dati.base.exception.ErrorMessages;
import lombok.Getter;

public enum ToolError {
    TOOL_NOT_FOUND        ("PARAM_ERROR",       "Tool not found"),
    TOOL_DISABLED         ("PARAM_ERROR",       "Tool is disabled"),
    PARAM_MISSING         ("PARAM_ERROR",       "Missing required parameter: {0}"),
    PARAM_INVALID         ("PARAM_ERROR",       "Invalid parameter: {0}"),
    ENTITY_NOT_FOUND       ("PARAM_ERROR",       "Table/column/term not found: {0}"),
    DATA_SOURCE_NOT_FOUND ("PARAM_ERROR",       "Data source not found: {0}"),
    // Scope errors carry the complete, resource-specific message supplied by the caller.
    SCOPE_VIOLATION       ("SCOPE_ERROR",       "{0}"),
    SQL_POLICY_VIOLATION  ("PERMISSION_DENIED", "SQL operation not allowed by policy: {0}"),
    SQL_EXECUTION_ERROR   ("SQL_ERROR",         "SQL execution failed: {0}");
    // TIMEOUT 预留，待超时检测功能实现后加入

    @Getter
    private final String category;
    private final String template;

    ToolError(String category, String template) {
        this.category = category;
        this.template = template;
    }

    public String format(Object... args) {
        return ErrorMessages.format(template, args);
    }
}
