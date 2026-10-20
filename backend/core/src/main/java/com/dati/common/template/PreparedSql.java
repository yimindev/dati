package com.dati.common.template;

import java.util.List;

/**
 * The result of SQL-mode rendering. Contains the SQL string with ? placeholders
 * and an ordered list of parameter bindings, matching the ? positions 1:1.
 * <p>
 * The consumer is responsible for obtaining a {@link java.sql.PreparedStatement}
 * and calling {@code setXxx(index, binding.value())} for each binding.
 * <p>
 * For display SQL (never executed), use {@link SqlRenderer#renderDisplay} instead of
 * string-replacing '?' in {@code sql}: template text may contain literal '?' characters
 * (e.g. {@code LIKE '%?%'} or PostgreSQL's JSONB {@code ?} operator).
 */
public record PreparedSql(String sql, List<ParamBinding> bindings) {
    public static PreparedSql of(String sql) {
        return new PreparedSql(sql, List.of());
    }
}
