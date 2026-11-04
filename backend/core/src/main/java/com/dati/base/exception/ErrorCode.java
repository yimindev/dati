package com.dati.base.exception;

import lombok.Getter;


@Getter
public enum ErrorCode {

    // ── Generic ──────────────────────────────────────────

    INVALID_PARAMETER("CM001", 400, "Request parameter error: {0}"),
    NOT_FOUND("CM002", 404, "Resource not found"),
    INTERNAL_ERROR("CM003", 500, "Internal server error"),
    METHOD_NOT_ALLOWED("CM004", 405, "Method not allowed: {0}"),

    // ── DataSource module (DS) ───────────────────────────

    DS_CONNECTION_FAILED("DS001", 400, "Database connection failed: {0}"),
    DS_NOT_FOUND("DS002", 404, "Data source not found: {0}"),
    DS_SQL_ERROR("DS003", 400, "SQL execution error: {0}"),
    DS_SYNC_FAILED("DS004", 500, "Failed to sync metadata for data source: {0}"),
    DS_UNSUPPORTED_TYPE("DS005", 400, "Unsupported database type: {0}"),
    DS_SCHEMA_DETECTION_FAILED("DS006", 400, "Database connected, but no usable default schema/database was found. Check the JDBC URL's currentSchema/search_path or database name, and ensure the target schema exists."),
    DS_TABLE_NOT_FOUND("DS007", 404, "Table not found: {0}"),
    DS_COLUMN_NOT_FOUND("DS008", 404, "Column not found: {0}"),
    DS_INVALID_JDBC_URL("DS009", 400, "Invalid JDBC URL: {0}. Expected a URL like {1}."),
    DS_JDBC_TYPE_MISMATCH("DS010", 400, "The JDBC URL does not match the selected database type {0}. Expected a URL starting with {1}, but got: {2}"),
    DS_HOST_UNRESOLVED("DS011", 400, "Host {0} cannot be resolved. Check the database host name."),
    DS_CONNECT_REFUSED("DS012", 400, "Connection to {0}:{1} was refused. Check that the database is running and the port is correct."),
    DS_CONNECT_TIMEOUT("DS013", 400, "Connection to {0}:{1} timed out. Check the host, port and network."),
    DS_AUTH_FAILED("DS014", 400, "Authentication failed for user {0}. Check the username and password."),
    DS_DATABASE_NOT_FOUND("DS015", 400, "Database {0} does not exist on {1}:{2}."),
    DS_ACCESS_DENIED("DS016", 400, "User {0} cannot access database {1}."),
    DS_TLS_FAILED("DS017", 400, "TLS negotiation with {0}:{1} failed. Check the server's SSL configuration."),
    DS_DRIVER_MISSING("DS018", 400, "No JDBC driver accepts this URL. Select the database type that matches the URL prefix."),
    DS_DATABASE_NOT_SPECIFIED("DS019", 400, "The JDBC URL does not contain a database name. Add the database to the URL, e.g. {0}"),
    DS_PASSWORD_REQUIRED_FOR_TEST("DS020", 400, "The connection details differ from the saved data source. Enter the password to test the new connection."),
    DS_PASSWORD_REQUIRED_FOR_UPDATE("DS021", 400, "The connection details changed. Re-enter the password to save the new connection."),
    DS_SESSION_FAILED("DS022", 400, "The server accepted the TCP connection but the JDBC session failed. Check the proxy, SSL and driver settings."),

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
    MS_SCOPE_ERROR("MS014", 400, "Data source or table not in scope: {0}"),
    MS_TOOL_DISABLED("MS015", 400, "Tool is disabled: {0}"),
    MS_SERVICE_STATUS_INVALID("MS016", 409, "Invalid status transition for MCP service: {0}"),
    MS_SERVICE_VERSION_NOT_FOUND("MS017", 404, "MCP service version not found: {0}"),
    MS_SERVICE_DATA_SCOPE_REQUIRED("MS018", 400, "Data scope is required when creating MCP service"),
    MS_SERVICE_DATA_SCOPE_EMPTY("MS019", 400, "Data scope is empty, cannot publish MCP service: {0}"),
    MS_TEMPLATE_QUOTED_VAR("MS020", 400, "Template variable(s) {0} are inside string literals; strings are auto-quoted, remove the surrounding quotes"),

    // ── Authentication module (AUTH) ──────────────────────

    AUTH_LOGIN_FAILED("AUTH001", 401, "Authentication failed: invalid credentials"),
    AUTH_TOKEN_INVALID("AUTH002", 401, "Token invalid or expired"),
    AUTH_USER_EXISTS("AUTH003", 409, "Username already exists: {0}"),
    AUTH_TYPE_UNSUPPORTED("AUTH004", 400, "Unsupported authentication type: {0}"),
    AUTH_KEY_FORBIDDEN("AUTH005", 403, "Cannot operate on API key of another user"),
    AUTH_KEY_INVALID_EXPIRY("AUTH006", 400, "expiresInDays must be one of 7/30/90/180/365"),
    AUTH_GUEST_DISABLED("AUTH007", 403, "Guest access is disabled"),
    AUTH_KEY_OWNER_INVALID("AUTH008", 403, "API keys can only be created for registered user accounts"),
    AUTH_TOKEN_REQUIRED("AUTH009", 401, "Authentication token is required"),

    // ── Permission module (PM) ───────────────────────────

    PERMISSION_DENIED("PM001", 403, "No permission to access this resource"),
    ACL_SELF_GRANT("PM002", 400, "Cannot grant permissions to yourself"),
    ACL_OWNER_GRANT("PM003", 400, "The resource owner already has full access");

    private final String code;
    private final int status;
    private final String template;

    ErrorCode(String code, int status, String template) {
        this.code = code;
        this.status = status;
        this.template = template;
    }
}
