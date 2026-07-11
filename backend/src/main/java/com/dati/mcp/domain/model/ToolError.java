package com.dati.mcp.domain.model;

import lombok.Getter;

import java.text.MessageFormat;

public enum ToolError {
    TOOL_NOT_FOUND        ("PARAM_ERROR",       "工具不存在"),
    TOOL_DISABLED         ("PARAM_ERROR",       "工具已禁用"),
    PARAM_MISSING         ("PARAM_ERROR",       "缺少必填参数：{0}"),
    DATA_SOURCE_NOT_FOUND ("PARAM_ERROR",       "数据源不存在"),
    SCOPE_VIOLATION       ("SCOPE_ERROR",       "数据源或表不在服务范围内：{0}"),
    SQL_POLICY_VIOLATION  ("PERMISSION_DENIED", "SQL 操作被策略禁止：{0}"),
    SQL_EXECUTION_ERROR   ("SQL_ERROR",         "SQL 执行失败：{0}");
    // TIMEOUT 预留，待超时检测功能实现后加入

    @Getter
    private final String category;
    private final String template;

    ToolError(String category, String template) {
        this.category = category;
        this.template = template;
    }

    public String format(Object... args) {
        return MessageFormat.format(template, args);
    }
}
