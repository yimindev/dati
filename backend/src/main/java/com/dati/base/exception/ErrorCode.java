package com.dati.base.exception;

import lombok.Getter;


@Getter
public enum ErrorCode {

    // ── Generic ──────────────────────────────────────────

    INVALID_PARAMETER("CM001", 400, "Request parameter error: {0}"),
    UNAUTHORIZED("CM002", 401, "Unauthorized"),
    FORBIDDEN("CM003", 403, "Access denied"),
    NOT_FOUND("CM004", 404, "Resource not found"),
    INTERNAL_ERROR("CM005", 500, "Internal server error"),

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

    FIELD_REQUIRED("VAL002", 400, "Field {0} is required");

    private final String code;
    private final int status;
    private final String template;

    ErrorCode(String code, int status, String template) {
        this.code = code;
        this.status = status;
        this.template = template;
    }
}
