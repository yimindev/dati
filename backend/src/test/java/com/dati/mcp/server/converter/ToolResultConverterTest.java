package com.dati.mcp.server.converter;

import com.dati.datasource.domain.model.ColumnDef;
import com.dati.datasource.domain.model.TableDef;
import com.dati.mcp.domain.model.ToolError;
import com.dati.mcp.domain.service.ToolExecuteException;
import com.dati.mcp.server.pojo.SqlExecution;
import com.dati.mcp.server.pojo.StatementResult;
import com.dati.mcp.server.pojo.TableMetadata;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ToolResultConverter tests")
class ToolResultConverterTest {

    private final ToolResultConverter converter = new ToolResultConverter();

    @Test
    @DisplayName("sql execution becomes text content plus structuredContent")
    void convertsSqlExecution() {
        SqlExecution execution = new SqlExecution("SELECT 1",
            List.of(StatementResult.select(List.of("a"), List.of(List.of(1)), 1)), null);
        McpSchema.CallToolResult result = converter.toResult(execution);
        assertFalse(result.isError());
        assertEquals(1, result.content().size());
        McpSchema.TextContent text = (McpSchema.TextContent) result.content().getFirst();
        assertNotNull(text.text());
        assertNotNull(result.structuredContent());
    }

    @Test
    @DisplayName("table metadata becomes structured content")
    void convertsTableMetadata() {
        TableDef table = new TableDef("public", "users", "user table", List.of(),
            List.of(new ColumnDef("id", "bigint", "primary key", List.of(), List.of("1", "2"))));
        TableMetadata metadata = new TableMetadata(List.of(table));
        McpSchema.CallToolResult result = converter.toResult(metadata);
        assertFalse(result.isError());
        assertNotNull(result.structuredContent());
    }

    @Test
    @DisplayName("search hit becomes structured content")
    void convertsSearchHit() {
        com.dati.mcp.server.pojo.SearchHit hit = new com.dati.mcp.server.pojo.SearchHit(
            List.of("orders"), List.of(), List.of());
        McpSchema.CallToolResult result = converter.toResult(hit);
        assertFalse(result.isError());
        assertNotNull(result.structuredContent());
    }

    @Test
    @DisplayName("tool execute exception becomes isError result with text message")
    void convertsException() {
        ToolExecuteException e = new ToolExecuteException(ToolError.SCOPE_VIOLATION, "table not in scope");
        McpSchema.CallToolResult result = converter.toError(e);
        assertTrue(result.isError());
        McpSchema.TextContent text = (McpSchema.TextContent) result.content().getFirst();
        assertTrue(text.text().contains("table not in scope"));
    }
}
