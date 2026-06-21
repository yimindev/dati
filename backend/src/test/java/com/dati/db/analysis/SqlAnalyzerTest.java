package com.dati.db.analysis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class SqlAnalyzerTest {

    // ============ TableRef ============

    @Test
    void tableRefWithoutSchema_qualifiedNameIsJustName() {
        TableRef ref = TableRef.of("orders");
        assertEquals("orders", ref.qualifiedName());
        assertNull(ref.schema());
        assertEquals("orders", ref.name());
    }

    @Test
    void tableRefWithSchema_qualifiedNameIncludesSchema() {
        TableRef ref = TableRef.of("public", "orders");
        assertEquals("public.orders", ref.qualifiedName());
        assertEquals("public", ref.schema());
        assertEquals("orders", ref.name());
    }

    @Test
    void tableRefEquality_sameNameAndSchemaAreEqual() {
        assertEquals(TableRef.of("orders"), TableRef.of("orders"));
        assertEquals(TableRef.of("public", "orders"), TableRef.of("public", "orders"));
        assertNotEquals(TableRef.of("public", "orders"), TableRef.of("orders"));
    }

    // ============ 操作类型检测 — DML（用例 1-12） ============

    static Stream<Arguments> dmlTypeCases() {
        return Stream.of(
                Arguments.of("SELECT * FROM orders", SqlOperationType.SELECT),
                Arguments.of("SELECT a.* FROM orders a JOIN users b ON a.user_id=b.id", SqlOperationType.SELECT),
                Arguments.of("SELECT * FROM orders WHERE user_id IN (SELECT id FROM users)", SqlOperationType.SELECT),
                Arguments.of("SELECT name FROM users UNION SELECT name FROM admins", SqlOperationType.SELECT),
                Arguments.of("WITH active AS (SELECT * FROM orders WHERE status=?) SELECT * FROM active", SqlOperationType.SELECT),
                Arguments.of("SELECT * FROM orders WHERE id=? FOR UPDATE", SqlOperationType.SELECT),
                Arguments.of("INSERT INTO orders (name,price) VALUES (?,?)", SqlOperationType.INSERT),
                Arguments.of("INSERT INTO orders SELECT * FROM tmp_orders", SqlOperationType.INSERT),
                Arguments.of("UPDATE orders SET status=? WHERE id=?", SqlOperationType.UPDATE),
                Arguments.of("UPDATE orders a JOIN users b SET a.status=? WHERE b.name=?", SqlOperationType.UPDATE),
                Arguments.of("DELETE FROM orders WHERE id=?", SqlOperationType.DELETE),
                Arguments.of("SELECT * FROM orders /* INSERT */ WHERE id=?", SqlOperationType.SELECT)
        );
    }

    @ParameterizedTest
    @MethodSource("dmlTypeCases")
    void shouldDetectDmlType(String sql, SqlOperationType expected) {
        SqlAnalysisResult result = SqlAnalyzer.analyze(sql);
        assertFalse(result.isMulti());
        assertEquals(expected, result.type());
        assertEquals(List.of(expected), result.statementTypes());
    }

    // ============ 操作类型检测 — DDL（用例 13-18） ============

    static Stream<Arguments> ddlTypeCases() {
        return Stream.of(
                Arguments.of("CREATE TABLE tmp (id INT)", SqlOperationType.DDL),
                Arguments.of("ALTER TABLE orders ADD COLUMN note TEXT", SqlOperationType.DDL),
                Arguments.of("DROP TABLE tmp_orders", SqlOperationType.DDL),
                Arguments.of("TRUNCATE TABLE orders", SqlOperationType.DDL),
                Arguments.of("CREATE INDEX idx_name ON orders(name)", SqlOperationType.DDL),
                Arguments.of("DROP INDEX idx_name", SqlOperationType.DDL)
        );
    }

    @ParameterizedTest
    @MethodSource("ddlTypeCases")
    void shouldDetectDdlType(String sql, SqlOperationType expected) {
        SqlAnalysisResult result = SqlAnalyzer.analyze(sql);
        assertFalse(result.isMulti());
        assertEquals(expected, result.type());
        assertEquals(List.of(expected), result.statementTypes());
    }

    // ============ 新增类型检测（用例 19-26） ============

    static Stream<Arguments> newTypeCases() {
        return Stream.of(
                Arguments.of("SHOW TABLES", SqlOperationType.METADATA, "SHOW TABLES"),
                Arguments.of("DESCRIBE orders", SqlOperationType.METADATA, "DESCRIBE"),
                Arguments.of("EXPLAIN SELECT * FROM orders", SqlOperationType.METADATA, "EXPLAIN"),
                Arguments.of("COMMIT", SqlOperationType.TRANSACTION, "COMMIT"),
                Arguments.of("ROLLBACK", SqlOperationType.TRANSACTION, "ROLLBACK"),
                Arguments.of("SET @var = 1", SqlOperationType.SET, "SET"),
                Arguments.of("CALL my_proc()", SqlOperationType.OTHER, "CALL")
        );
    }

    @ParameterizedTest
    @MethodSource("newTypeCases")
    void shouldDetectNewType(String sql, SqlOperationType expectedType, String description) {
        SqlAnalysisResult result = SqlAnalyzer.analyze(sql);
        assertFalse(result.isMulti(), description + " should be single statement");
        assertEquals(expectedType, result.type(), description);
        assertEquals(List.of(expectedType), result.statementTypes(), description);
    }

    // ============ 表提取 — 基本（用例 27-34） ============

    static Stream<Arguments> basicTableCases() {
        return Stream.of(
                Arguments.of("SELECT * FROM orders",
                        Set.of(TableRef.of("orders"))),
                Arguments.of("SELECT * FROM public.orders",
                        Set.of(TableRef.of("public", "orders"))),
                Arguments.of("SELECT * FROM orders JOIN users ON orders.uid=users.id",
                        Set.of(TableRef.of("orders"), TableRef.of("users"))),
                Arguments.of("SELECT * FROM orders, users",
                        Set.of(TableRef.of("orders"), TableRef.of("users"))),
                Arguments.of("SELECT * FROM orders o, users u",
                        Set.of(TableRef.of("orders"), TableRef.of("users"))),
                Arguments.of("INSERT INTO orders VALUES (?)",
                        Set.of(TableRef.of("orders"))),
                Arguments.of("UPDATE orders SET x=?",
                        Set.of(TableRef.of("orders"))),
                Arguments.of("DELETE FROM orders WHERE id=?",
                        Set.of(TableRef.of("orders")))
        );
    }

    @ParameterizedTest
    @MethodSource("basicTableCases")
    void shouldExtractBasicTables(String sql, Set<TableRef> expected) {
        SqlAnalysisResult result = SqlAnalyzer.analyze(sql);
        assertEquals(expected, result.tables(),
                "Expected tables " + expected + " but got " + result.tables() + " for: " + sql);
    }

    // ============ 表提取 — 子查询 & CTE（用例 35-40） ============

    static Stream<Arguments> subqueryTableCases() {
        return Stream.of(
                Arguments.of("SELECT * FROM orders WHERE uid IN (SELECT id FROM users)",
                        Set.of(TableRef.of("orders"), TableRef.of("users"))),
                Arguments.of("SELECT * FROM (SELECT * FROM orders) t",
                        Set.of(TableRef.of("orders"))),
                Arguments.of("SELECT * FROM orders JOIN (SELECT id FROM users) u ON orders.uid=u.id",
                        Set.of(TableRef.of("orders"), TableRef.of("users"))),
                Arguments.of("WITH recent AS (SELECT * FROM orders WHERE date>?) SELECT * FROM recent",
                        Set.of(TableRef.of("orders"))),
                Arguments.of("SELECT * FROM a WHERE x IN (SELECT y FROM b WHERE z IN (SELECT w FROM c))",
                        Set.of(TableRef.of("a"), TableRef.of("b"), TableRef.of("c"))),
                Arguments.of("INSERT INTO archive SELECT * FROM orders",
                        Set.of(TableRef.of("archive"), TableRef.of("orders")))
        );
    }

    @ParameterizedTest
    @MethodSource("subqueryTableCases")
    void shouldExtractSubqueryTables(String sql, Set<TableRef> expected) {
        SqlAnalysisResult result = SqlAnalyzer.analyze(sql);
        assertEquals(expected, result.tables(),
                "Expected tables " + expected + " but got " + result.tables() + " for: " + sql);
    }

    // ============ 表提取 — 边界（用例 41-45） ============

    @Test
    void shouldNotExtractFunctionAsTable() {
        SqlAnalysisResult result = SqlAnalyzer.analyze("SELECT COUNT(*) FROM orders");
        assertEquals(Set.of(TableRef.of("orders")), result.tables());
    }

    @Test
    void shouldStripQuotesFromTableName() {
        SqlAnalysisResult result = SqlAnalyzer.analyze("SELECT * FROM \"Orders\"");
        assertEquals(Set.of(TableRef.of("Orders")), result.tables());
    }

    @Test
    void shouldHandleBacktickTableName() {
        SqlAnalysisResult result = SqlAnalyzer.analyze("SELECT * FROM `order-items`");
        assertEquals(Set.of(TableRef.of("order-items")), result.tables());
    }

    @Test
    void shouldReturnEmptyTablesForNoTableQuery() {
        SqlAnalysisResult result = SqlAnalyzer.analyze("SELECT 1+1");
        assertEquals(Set.of(), result.tables());
    }

    @Test
    void shouldExtractDdlTargetTable() {
        SqlAnalysisResult result = SqlAnalyzer.analyze("DROP TABLE orders");
        assertEquals(Set.of(TableRef.of("orders")), result.tables());
    }

    // ============ 模板渲染后 SQL — 含 ? 占位符（用例 46-50） ============

    static Stream<Arguments> templateRenderedCases() {
        return Stream.of(
                Arguments.of("SELECT * FROM orders WHERE status=?",
                        Set.of(TableRef.of("orders")), SqlOperationType.SELECT),
                Arguments.of("INSERT INTO orders (name) VALUES (?)",
                        Set.of(TableRef.of("orders")), SqlOperationType.INSERT),
                Arguments.of("SELECT * FROM orders LIMIT ?",
                        Set.of(TableRef.of("orders")), SqlOperationType.SELECT),
                Arguments.of("SELECT * FROM orders WHERE id IN (?,?,?)",
                        Set.of(TableRef.of("orders")), SqlOperationType.SELECT),
                Arguments.of("SELECT * FROM orders JOIN users ON orders.uid=?",
                        Set.of(TableRef.of("orders"), TableRef.of("users")), SqlOperationType.SELECT)
        );
    }

    @ParameterizedTest
    @MethodSource("templateRenderedCases")
    void shouldAnalyzeTemplateRenderedSql(String sql, Set<TableRef> expectedTables, SqlOperationType expectedType) {
        SqlAnalysisResult result = SqlAnalyzer.analyze(sql);
        assertFalse(result.isMulti());
        assertEquals(expectedType, result.type());
        assertEquals(List.of(expectedType), result.statementTypes());
        assertEquals(expectedTables, result.tables());
    }

    // ============ 多语句检测（用例 51-56） ============

    @Test
    void singleSelectIsNotMulti() {
        SqlAnalysisResult result = SqlAnalyzer.analyze("SELECT * FROM orders");
        assertFalse(result.isMulti());
        assertEquals(SqlOperationType.SELECT, result.type());
        assertEquals(List.of(SqlOperationType.SELECT), result.statementTypes());
    }

    @Test
    void twoSelectsAreMulti() {
        SqlAnalysisResult result = SqlAnalyzer.analyze("SELECT * FROM orders; SELECT * FROM users");
        assertTrue(result.isMulti());
        assertEquals(SqlOperationType.MULTI, result.type());
        assertEquals(List.of(SqlOperationType.SELECT, SqlOperationType.SELECT), result.statementTypes());
    }

    @Test
    void mixedDdlAndSelectAreMulti() {
        SqlAnalysisResult result = SqlAnalyzer.analyze("DROP TABLE tmp; SELECT * FROM orders");
        assertTrue(result.isMulti());
        assertEquals(SqlOperationType.MULTI, result.type());
        assertEquals(List.of(SqlOperationType.DDL, SqlOperationType.SELECT), result.statementTypes());
    }

    @Test
    void trailingSemicolonIsSingleStatement() {
        SqlAnalysisResult result = SqlAnalyzer.analyze("SELECT * FROM orders;");
        assertFalse(result.isMulti());
        assertEquals(SqlOperationType.SELECT, result.type());
        assertEquals(List.of(SqlOperationType.SELECT), result.statementTypes());
    }

    @Test
    void emptyTrailingStatementIsIgnored() {
        SqlAnalysisResult result = SqlAnalyzer.analyze("SELECT * FROM orders; ;");
        assertFalse(result.isMulti());
        assertEquals(SqlOperationType.SELECT, result.type());
        assertEquals(List.of(SqlOperationType.SELECT), result.statementTypes());
    }

    @Test
    void multiWithCommitAndRollback() {
        // COMMIT and ROLLBACK are parseable; BEGIN is not, so this tests the achievable combination
        SqlAnalysisResult result = SqlAnalyzer.analyze("UPDATE accounts SET balance=100; COMMIT");
        assertTrue(result.isMulti());
        assertEquals(SqlOperationType.MULTI, result.type());
        assertEquals(List.of(SqlOperationType.UPDATE, SqlOperationType.TRANSACTION), result.statementTypes());
    }

    // ============ 解析失败 / 异常处理（用例 57-60） ============

    @Test
    void shouldReturnOtherForNull() {
        SqlAnalysisResult result = SqlAnalyzer.analyze(null);
        assertEquals(SqlOperationType.OTHER, result.type());
        assertEquals(List.of(), result.statementTypes());
        assertEquals(Set.of(), result.tables());
    }

    @Test
    void shouldReturnOtherForEmptyString() {
        SqlAnalysisResult result = SqlAnalyzer.analyze("");
        assertEquals(SqlOperationType.OTHER, result.type());
        assertEquals(List.of(), result.statementTypes());
        assertEquals(Set.of(), result.tables());
    }

    @Test
    void shouldReturnOtherForGarbledInput() {
        SqlAnalysisResult result = SqlAnalyzer.analyze("NOT A SQL STATEMENT !!!");
        assertEquals(SqlOperationType.OTHER, result.type());
        assertEquals(List.of(), result.statementTypes());
        assertEquals(Set.of(), result.tables());
    }

    @Test
    void shouldReturnOtherForInvalidSql() {
        SqlAnalysisResult result = SqlAnalyzer.analyze("SELECT * FROM WHERE id=?");
        assertEquals(SqlOperationType.OTHER, result.type());
        assertEquals(List.of(), result.statementTypes());
        assertEquals(Set.of(), result.tables());
    }

    // ============ 安全 — 注释绕过（用例 61-63） ============

    @Test
    void shouldIgnoreLineCommentWhenDetectingType() {
        SqlAnalysisResult result = SqlAnalyzer.analyze("SELECT * FROM orders -- DROP TABLE users");
        assertEquals(SqlOperationType.SELECT, result.type());
        assertEquals(Set.of(TableRef.of("orders")), result.tables());
    }

    @Test
    void shouldIgnoreBlockCommentWhenDetectingType() {
        SqlAnalysisResult result = SqlAnalyzer.analyze("SELECT * FROM orders /* INSERT INTO users */ WHERE id=?");
        assertEquals(SqlOperationType.SELECT, result.type());
        assertEquals(Set.of(TableRef.of("orders")), result.tables());
    }

    @Test
    void shouldDetectDdlAfterLineComment() {
        SqlAnalysisResult result = SqlAnalyzer.analyze("-- SELECT * FROM orders\nDROP TABLE users");
        assertEquals(SqlOperationType.DDL, result.type());
        assertEquals(Set.of(TableRef.of("users")), result.tables());
    }

    // ============ isMulti() 边界 ============

    @Test
    void isMultiReturnsFalseForSingleStatement() {
        SqlAnalysisResult result = SqlAnalyzer.analyze("SELECT 1");
        assertFalse(result.isMulti());
    }

    @Test
    void isMultiReturnsTrueForMultipleStatements() {
        SqlAnalysisResult result = SqlAnalyzer.analyze("SELECT 1; SELECT 2");
        assertTrue(result.isMulti());
    }

    @Test
    void isMultiReturnsFalseForEmpty() {
        SqlAnalysisResult result = SqlAnalyzer.analyze(null);
        assertFalse(result.isMulti());
    }

    @Test
    void isMultiReturnsFalseForParseError() {
        SqlAnalysisResult result = SqlAnalyzer.analyze("GARBLED");
        assertFalse(result.isMulti());
    }

    // ============ BEGIN / START TRANSACTION 预扫描 ============

    @Test
    void beginIsDetectedAsTransaction() {
        SqlAnalysisResult result = SqlAnalyzer.analyze("BEGIN");
        assertFalse(result.isMulti());
        assertEquals(SqlOperationType.TRANSACTION, result.type());
        assertEquals(List.of(SqlOperationType.TRANSACTION), result.statementTypes());
    }

    @Test
    void startTransactionIsDetectedAsTransaction() {
        SqlAnalysisResult result = SqlAnalyzer.analyze("START TRANSACTION");
        assertFalse(result.isMulti());
        assertEquals(SqlOperationType.TRANSACTION, result.type());
        assertEquals(List.of(SqlOperationType.TRANSACTION), result.statementTypes());
    }

    @Test
    void beginThenSelectIsMulti() {
        SqlAnalysisResult result = SqlAnalyzer.analyze("BEGIN; SELECT * FROM orders");
        assertTrue(result.isMulti());
        assertEquals(SqlOperationType.MULTI, result.type());
        assertEquals(List.of(SqlOperationType.TRANSACTION, SqlOperationType.SELECT), result.statementTypes());
        assertEquals(Set.of(TableRef.of("orders")), result.tables());
    }

    @Test
    void beginThenUpdateThenCommitIsMulti() {
        SqlAnalysisResult result = SqlAnalyzer.analyze("BEGIN; UPDATE accounts SET balance=100; COMMIT");
        assertTrue(result.isMulti());
        assertEquals(SqlOperationType.MULTI, result.type());
        assertEquals(List.of(SqlOperationType.TRANSACTION, SqlOperationType.UPDATE, SqlOperationType.TRANSACTION),
                result.statementTypes());
        assertEquals(Set.of(TableRef.of("accounts")), result.tables());
    }

    @Test
    void startTransactionThenInsertThenCommitIsMulti() {
        SqlAnalysisResult result = SqlAnalyzer.analyze("START TRANSACTION; INSERT INTO t VALUES (1); COMMIT");
        assertTrue(result.isMulti());
        assertEquals(SqlOperationType.MULTI, result.type());
        assertEquals(List.of(SqlOperationType.TRANSACTION, SqlOperationType.INSERT, SqlOperationType.TRANSACTION),
                result.statementTypes());
    }
}
