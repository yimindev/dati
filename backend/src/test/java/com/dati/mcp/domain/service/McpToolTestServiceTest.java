package com.dati.mcp.domain.service;

import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.domain.model.ToolConfig.ExecuteSqlConfig;
import com.dati.mcp.domain.model.ToolError;
import com.dati.mcp.repository.dao.McpServiceDataScopeDAO;
import com.dati.mcp.server.pojo.SqlExecution;
import com.dati.mcp.server.pojo.ToolTestData;
import com.dati.mcp.server.pojo.ToolTestRequest;
import com.dati.mcp.server.pojo.ToolTestResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("McpToolTestService 单元测试")
class McpToolTestServiceTest {

    @Mock
    private ToolResolver toolResolver;

    @Mock
    private McpServiceDataScopeDAO scopeDAO;

    @Mock
    private ToolExecutor toolExecutor;

    private McpToolTestService service;

    @BeforeEach
    void setUp() {
        when(toolExecutor.getToolType()).thenReturn(McpToolType.EXECUTE_SQL);
        service = new McpToolTestService(toolResolver, scopeDAO, List.of(toolExecutor));
    }

    @Test
    @DisplayName("正常执行成功 → 返回 success=true 且含 data")
    void shouldReturnSuccessResponse() {
        ToolResolver.ResolvedTool resolved = new ToolResolver.ResolvedTool(
            McpToolType.EXECUTE_SQL, true, new ExecuteSqlConfig(), true);
        when(toolResolver.resolve("svc", "EXECUTE_SQL")).thenReturn(resolved);
        when(scopeDAO.findAllByServiceId("svc")).thenReturn(List.of());
        ToolTestData data = new SqlExecution("SELECT 1", List.of(), null);
        when(toolExecutor.execute(any())).thenReturn(data);

        ToolTestResponse resp = service.test("svc", "EXECUTE_SQL",
            new ToolTestRequest(Map.of("data_source_id", "ds-1", "sql", "SELECT 1")));

        assertThat(resp.success()).isTrue();
        assertThat(resp.data()).isEqualTo(data);
        assertThat(resp.error()).isNull();
        assertThat(resp.executionTimeMs()).isNotNegative();
    }

    @Test
    @DisplayName("executor 抛 RuntimeException → 上抛不捕获")
    void shouldNotCatchRuntimeException() {
        ToolResolver.ResolvedTool resolved = new ToolResolver.ResolvedTool(
            McpToolType.EXECUTE_SQL, true, new ExecuteSqlConfig(), true);
        when(toolResolver.resolve("svc", "EXECUTE_SQL")).thenReturn(resolved);
        when(scopeDAO.findAllByServiceId("svc")).thenReturn(List.of());
        when(toolExecutor.execute(any())).thenThrow(new NullPointerException("boom"));

        assertThatThrownBy(() ->
            service.test("svc", "EXECUTE_SQL",
                new ToolTestRequest(Map.of("data_source_id", "ds-1", "sql", "SELECT 1"))))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("boom");
    }

    @ParameterizedTest
    @CsvSource({
        "TOOL_NOT_FOUND, PARAM_ERROR",
        "TOOL_DISABLED, PARAM_ERROR",
        "PARAM_MISSING, PARAM_ERROR",
        "DATA_SOURCE_NOT_FOUND, PARAM_ERROR",
        "SCOPE_VIOLATION, SCOPE_ERROR",
        "SQL_POLICY_VIOLATION, PERMISSION_DENIED",
        "SQL_EXECUTION_ERROR, SQL_ERROR",
    })
    @DisplayName("ToolExecuteException → errorCategory 映射正确")
    void shouldMapToolExecuteExceptionToCorrectCategory(ToolError toolError, String expectedCategory) {
        ToolResolver.ResolvedTool resolved = new ToolResolver.ResolvedTool(
            McpToolType.EXECUTE_SQL, true, new ExecuteSqlConfig(), true);
        when(toolResolver.resolve("svc", "EXECUTE_SQL")).thenReturn(resolved);
        when(scopeDAO.findAllByServiceId("svc")).thenReturn(List.of());
        when(toolExecutor.execute(any())).thenThrow(new ToolExecuteException(toolError, "test"));

        ToolTestResponse resp = service.test("svc", "EXECUTE_SQL",
            new ToolTestRequest(Map.of("data_source_id", "ds-1", "sql", "SELECT 1")));

        assertThat(resp.success()).isFalse();
        assertThat(resp.error()).isNotNull();
        assertThat(resp.error().errorCategory()).isEqualTo(expectedCategory);
    }
}
