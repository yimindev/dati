package com.dati.mcp.domain.service;

import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.domain.model.ToolConfig.ExecuteSqlConfig;
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
    @DisplayName("executor 抛非 DatiException → 兜底返回 success=false + SQL_ERROR")
    void shouldCatchRuntimeException() {
        ToolResolver.ResolvedTool resolved = new ToolResolver.ResolvedTool(
            McpToolType.EXECUTE_SQL, true, new ExecuteSqlConfig(), true);
        when(toolResolver.resolve("svc", "EXECUTE_SQL")).thenReturn(resolved);
        when(scopeDAO.findAllByServiceId("svc")).thenReturn(List.of());
        when(toolExecutor.execute(any())).thenThrow(new NullPointerException("boom"));

        ToolTestResponse resp = service.test("svc", "EXECUTE_SQL",
            new ToolTestRequest(Map.of("data_source_id", "ds-1", "sql", "SELECT 1")));

        assertThat(resp.success()).isFalse();
        assertThat(resp.data()).isNull();
        assertThat(resp.error()).isNotNull();
        assertThat(resp.error().errorCategory()).isEqualTo("INTERNAL_ERROR");
        assertThat(resp.error().message()).contains("boom");
        assertThat(resp.executionTimeMs()).isNotNegative();
    }

    @ParameterizedTest
    @CsvSource({
        "MS_SCOPE_ERROR, SCOPE_ERROR",
        "MS_SQL_POLICY_VIOLATION, PERMISSION_DENIED",
        "MS_TOOL_DISABLED, PARAM_ERROR",
        "DS_SQL_ERROR, SQL_ERROR",
        "DS_CONNECTION_FAILED, SQL_ERROR",
        "INTERNAL_ERROR, INTERNAL_ERROR",
    })
    @DisplayName("DatiException → errorCategory 映射正确")
    void shouldMapDatiExceptionToCorrectCategory(ErrorCode code, String expectedCategory) {
        ToolResolver.ResolvedTool resolved = new ToolResolver.ResolvedTool(
            McpToolType.EXECUTE_SQL, true, new ExecuteSqlConfig(), true);
        when(toolResolver.resolve("svc", "EXECUTE_SQL")).thenReturn(resolved);
        when(scopeDAO.findAllByServiceId("svc")).thenReturn(List.of());
        when(toolExecutor.execute(any())).thenThrow(new DatiException(code, "test"));

        ToolTestResponse resp = service.test("svc", "EXECUTE_SQL",
            new ToolTestRequest(Map.of("data_source_id", "ds-1", "sql", "SELECT 1")));

        assertThat(resp.success()).isFalse();
        assertThat(resp.error()).isNotNull();
        assertThat(resp.error().errorCategory()).isEqualTo(expectedCategory);
    }
}
