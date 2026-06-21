package com.dati.base.exception;

import lombok.Getter;


@Getter
public enum ErrorCode {

    // ── Generic ──────────────────────────────────────────

    INVALID_PARAMETER("CM001", 400, "Request parameter error: {0}"),
    NOT_FOUND("CM002", 404, "Resource not found"),
    INTERNAL_ERROR("CM003", 500, "Internal server error"),

    // ── DataSource module (DS) ───────────────────────────

    DS_CONNECTION_FAILED("DS001", 400, "Database connection failed: {0}"),
    DS_NOT_FOUND("DS002", 404, "Data source not found: {0}"),
    DS_SQL_ERROR("DS003", 400, "SQL execution error: {0}"),
    DS_SYNC_FAILED("DS004", 500, "Failed to sync metadata for data source: {0}"),

    // ── Semantic module (SM) ─────────────────────────────

    SM_SUBJECT_NOT_FOUND("SM001", 404, "Subject not found: {0}"),
    SM_TERM_NOT_FOUND("SM002", 404, "Term not found: {0}"),
    SM_TABLE_NOT_IN_SUBJECT("SM003", 400, "Table {0} does not belong to subject {1}"),
    SM_TERM_RELATION_INVALID("SM004", 400, "Invalid term relation: {0}"),
    SM_TABLE_ALREADY_ASSOCIATED("SM005", 400, "Table {0} is already associated with subject {1}"),
    SM_ASSOCIATION_NOT_FOUND("SM006", 404, "Association between subject {0} and table {1} not found"),

    FIELD_REQUIRED("VAL002", 400, "Field {0} is required"),

    // ── MCP module (MS) ──────────────────────────────────

    MS_SERVICE_NOT_FOUND("MS001", 404, "MCP service not found: {0}"),
    MS_TOOL_NOT_FOUND("MS002", 404, "MCP tool not found: {0}"),
    MS_TOOL_NAME_EXISTS("MS003", 409, "Tool name already exists in this service: {0}"),
    MS_TOOL_NAME_INVALID("MS004", 400, "Tool name must be 1-128 chars, allowed: A-Z, a-z, 0-9, _, -, ."),

    MS_PROMPT_NOT_FOUND("MS005", 404, "MCP prompt not found: {0}"),
    MS_PROMPT_NAME_EXISTS("MS006", 409, "Prompt name already exists in this service: {0}"),
    MS_PROMPT_ARG_MISMATCH("MS007", 400, "Prompt parameter mismatch: {0}"),
    MS_TEMPLATE_SYNTAX_ERROR("MS008", 400, "Template syntax error in {0}: {1}"),
    MS_TOOL_ARG_MISMATCH("MS009", 400, "Tool parameter mismatch: {0}"),
    MS_SERVICE_CODE_EXISTS("MS010", 409, "Service code already exists: {0}"),
    MS_SERVICE_CODE_INVALID("MS011", 400, "Service code must be 1-64 chars, lowercase letters, digits, hyphens and underscores only"),
    MS_SERVICE_CODE_REQUIRED("MS012", 400, "Service code is required"),
    MS_SQL_POLICY_VIOLATION("MS013", 403, "SQL operation not allowed by policy: {0}"),

    // ── Authentication module (AUTH) ──────────────────────

    AUTH_LOGIN_FAILED("AUTH001", 401, "Authentication failed: invalid credentials"),
    AUTH_TOKEN_INVALID("AUTH002", 401, "Token invalid or expired"),
    AUTH_USER_EXISTS("AUTH003", 409, "Username already exists: {0}"),
    AUTH_TYPE_UNSUPPORTED("AUTH004", 400, "Unsupported authentication type: {0}");

    private final String code;
    private final int status;
    private final String template;

    ErrorCode(String code, int status, String template) {
        this.code = code;
        this.status = status;
        this.template = template;
    }
}
