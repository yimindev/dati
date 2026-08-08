package com.dati.mcp.server.converter;

import com.dati.datasource.domain.model.ColumnDef;
import com.dati.datasource.domain.model.TableDef;
import com.dati.mcp.domain.model.ToolError;
import com.dati.mcp.domain.service.ToolExecuteException;
import com.dati.mcp.server.pojo.SqlExecution;
import com.dati.mcp.server.pojo.StatementResult;
import com.dati.mcp.server.pojo.TableMetadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ToolResultConverter tests")
class ToolResultConverterTest {

    private final ToolResultConverter converter = new ToolResultConverter();

    @Test
    @DisplayName("sql execution becomes text content plus structuredContent")
    void convertsSqlExecution() {
        SqlExecution execution = new SqlExecution("SELECT 1",
            List.of(StatementResult.select(List.of("a"), List.of(List.of(1)), 1)), null);
        Map<String, Object> result = converter.toResult(execution);
        assertFalse((boolean) result.get("isError"));
        assertEquals(1, ((List<?>) result.get("content")).size());
        Map<?, ?> text = (Map<?, ?>) ((List<?>) result.get("content")).getFirst();
        assertEquals("text", text.get("type"));
        assertNotNull(result.get("structuredContent"));
    }

    @Test
    @DisplayName("table metadata becomes structured content")
    void convertsTableMetadata() {
        TableDef table = new TableDef("public", "users", "user table", List.of(),
            List.of(new ColumnDef("id", "bigint", "primary key", List.of(), List.of("1", "2"))));
        TableMetadata metadata = new TableMetadata(List.of(table));
        Map<String, Object> result = converter.toResult(metadata);
        assertFalse((boolean) result.get("isError"));
        assertNotNull(result.get("structuredContent"));
    }

    @Test
    @DisplayName("search hit becomes structured content")
    void convertsSearchHit() {
        com.dati.mcp.server.pojo.SearchHit hit = new com.dati.mcp.server.pojo.SearchHit(
            List.of("orders"), List.of(), List.of());
        Map<String, Object> result = converter.toResult(hit);
        assertFalse((boolean) result.get("isError"));
        assertNotNull(result.get("structuredContent"));
    }

    @Test
    @DisplayName("tool execute exception becomes isError result with text message")
    void convertsException() {
        ToolExecuteException e = new ToolExecuteException(ToolError.SCOPE_VIOLATION, "table not in scope");
        Map<String, Object> result = converter.toError(e);
        assertTrue((boolean) result.get("isError"));
        Map<?, ?> text = (Map<?, ?>) ((List<?>) result.get("content")).getFirst();
        assertEquals("text", text.get("type"));
        assertTrue(((String) text.get("text")).contains("table not in scope"));
    }
}
