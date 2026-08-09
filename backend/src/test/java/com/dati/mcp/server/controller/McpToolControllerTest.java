package com.dati.mcp.server.controller;

import com.dati.TestFixtures;
import com.dati.mcp.domain.model.McpCustomTool;
import com.dati.mcp.domain.model.McpToolType;
import com.dati.mcp.domain.service.McpToolService;
import com.dati.mcp.domain.service.McpToolTestService;
import com.dati.mcp.domain.service.ToolsResult;
import com.dati.mcp.server.assembler.McpToolAssembler;
import com.dati.mcp.server.pojo.McpToolVO;
import com.dati.mcp.server.pojo.SqlExecution;
import com.dati.mcp.server.pojo.ToolTestError;
import com.dati.mcp.server.pojo.ToolTestResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(McpToolController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("McpToolController integration tests")
class McpToolControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private McpToolService mcpToolService;

    @MockitoBean
    private McpToolAssembler mcpToolAssembler;

    @MockitoBean
    private McpToolTestService mcpToolTestService;

    @BeforeEach
    void setUp() {
        // listTools returns prebuilt + custom
        McpToolVO prebuilt = new McpToolVO();
        prebuilt.setId("SEARCH_METADATA");
        prebuilt.setToolType(McpToolType.SEARCH_METADATA);
        prebuilt.setName("search_metadata");
        prebuilt.setDescription("Search metadata");
        prebuilt.setEnabled(true);

        McpToolVO custom = new McpToolVO();
        custom.setId(TestFixtures.TEST_MCP_CUSTOM_TOOL_ID);
        custom.setToolType(McpToolType.PARAMETERIZED_SQL);
        custom.setName("list_tasks");
        custom.setEnabled(true);

        ToolsResult result = new ToolsResult(List.of(), List.of());
        when(mcpToolService.listTools(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(result);

        McpToolVO prebuiltVO = new McpToolVO();
        prebuiltVO.setId("SEARCH_METADATA");
        prebuiltVO.setToolType(McpToolType.SEARCH_METADATA);
        prebuiltVO.setEnabled(true);
        McpToolVO customVO = new McpToolVO();
        customVO.setId(TestFixtures.TEST_MCP_CUSTOM_TOOL_ID);
        customVO.setToolType(McpToolType.PARAMETERIZED_SQL);
        customVO.setName("list_tasks");

        when(mcpToolAssembler.toPrebuiltVOList(any())).thenReturn(List.of(prebuiltVO, new McpToolVO(), new McpToolVO()));
        when(mcpToolAssembler.toCustomVOList(any())).thenReturn(List.of(customVO));
    }

    @Test
    @DisplayName("GET /tools - returns prebuilt and custom tools grouped")
    void listTools_shouldReturnGrouped() throws Exception {
        mockMvc.perform(get("/v1/mcp-services/{serviceId}/tools", TestFixtures.TEST_MCP_SERVICE_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.prebuilt").isArray())
            .andExpect(jsonPath("$.prebuilt[0].id").value("SEARCH_METADATA"))
            .andExpect(jsonPath("$.custom").isArray())
            .andExpect(jsonPath("$.custom[0].id").value(TestFixtures.TEST_MCP_CUSTOM_TOOL_ID));
    }

    @Test
    @DisplayName("PUT /tools/{toolId} - update prebuilt tool (enabled flag)")
    void updateTool_togglePrebuilt() throws Exception {
        doNothing().when(mcpToolService).updatePrebuiltTool(eq(TestFixtures.TEST_MCP_SERVICE_ID), any(), any());

        mockMvc.perform(put("/v1/mcp-services/{serviceId}/tools/{toolId}", TestFixtures.TEST_MCP_SERVICE_ID, "SEARCH_METADATA")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tool_type\":\"SEARCH_METADATA\",\"enabled\":false}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("SEARCH_METADATA"));
    }

    @Test
    @DisplayName("PUT /tools/{toolId} - update custom tool")
    void updateTool_custom() throws Exception {
        when(mcpToolAssembler.toModel(any(com.dati.mcp.server.pojo.CustomToolRequest.class))).thenReturn(new McpCustomTool());
        doNothing().when(mcpToolService).updateCustomTool(any());

        mockMvc.perform(put("/v1/mcp-services/{serviceId}/tools/{toolId}", TestFixtures.TEST_MCP_SERVICE_ID, TestFixtures.TEST_MCP_CUSTOM_TOOL_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tool_type\":\"PARAMETERIZED_SQL\",\"name\":\"list_tasks\",\"enabled\":false}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(TestFixtures.TEST_MCP_CUSTOM_TOOL_ID));
    }

    @Test
    @DisplayName("POST /tools - create custom tool")
    void createTool_shouldReturnId() throws Exception {
        when(mcpToolService.createCustomTool(eq(TestFixtures.TEST_MCP_SERVICE_ID), any()))
            .thenReturn(TestFixtures.TEST_MCP_CUSTOM_TOOL_ID);

        mockMvc.perform(post("/v1/mcp-services/{serviceId}/tools", TestFixtures.TEST_MCP_SERVICE_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tool_type\":\"PARAMETERIZED_SQL\",\"name\":\"list_tasks\",\"description\":\"查询任务\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(TestFixtures.TEST_MCP_CUSTOM_TOOL_ID));
    }

    @Test
    @DisplayName("POST /tools - create without description rejected with 400 (BUG-20260802-001)")
    void createTool_missingDescription_shouldReturn400() throws Exception {
        mockMvc.perform(post("/v1/mcp-services/{serviceId}/tools", TestFixtures.TEST_MCP_SERVICE_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tool_type\":\"PARAMETERIZED_SQL\",\"name\":\"list_tasks\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("CM001"));
    }

    @Test
    @DisplayName("DELETE /tools/{toolId} - delete custom tool")
    void deleteTool_shouldReturnId() throws Exception {
        doNothing().when(mcpToolService).deleteCustomTool(TestFixtures.TEST_MCP_SERVICE_ID, TestFixtures.TEST_MCP_CUSTOM_TOOL_ID);

        mockMvc.perform(delete("/v1/mcp-services/{serviceId}/tools/{toolId}", TestFixtures.TEST_MCP_SERVICE_ID, TestFixtures.TEST_MCP_CUSTOM_TOOL_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(TestFixtures.TEST_MCP_CUSTOM_TOOL_ID));
    }

    @Test
    @DisplayName("POST /tools/{toolId}/test - returns ToolTestResponse")
    void testTool_shouldReturnToolTestResponse() throws Exception {
        SqlExecution data = new SqlExecution("SELECT 1", List.of(), null);
        ToolTestResponse mockResp = new ToolTestResponse(true, 42, data, null);
        when(mcpToolTestService.test(eq(TestFixtures.TEST_MCP_SERVICE_ID), eq("EXECUTE_SQL"), any()))
            .thenReturn(mockResp);

        mockMvc.perform(post("/v1/mcp-services/{serviceId}/tools/{toolId}/test",
                TestFixtures.TEST_MCP_SERVICE_ID, "EXECUTE_SQL")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"arguments\":{\"data_source_id\":\"ds-1\",\"sql\":\"SELECT 1\"}}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.execution_time_ms").value(42))
            .andExpect(jsonPath("$.data.type").value("SQL_EXECUTION"))
            .andExpect(jsonPath("$.data.executed_sql").value("SELECT 1"))
            .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    @DisplayName("POST /tools/{toolId}/test - returns failure response")
    void testTool_shouldReturnErrorResponse() throws Exception {
        ToolTestError error = new ToolTestError("SCOPE_ERROR", "Data source not in scope");
        ToolTestResponse mockResp = new ToolTestResponse(false, 15, null, error);
        when(mcpToolTestService.test(eq(TestFixtures.TEST_MCP_SERVICE_ID), eq("EXECUTE_SQL"), any()))
            .thenReturn(mockResp);

        mockMvc.perform(post("/v1/mcp-services/{serviceId}/tools/{toolId}/test",
                TestFixtures.TEST_MCP_SERVICE_ID, "EXECUTE_SQL")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"arguments\":{\"data_source_id\":\"ds-1\",\"sql\":\"SELECT 1\"}}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.execution_time_ms").value(15))
            .andExpect(jsonPath("$.error.error_category").value("SCOPE_ERROR"))
            .andExpect(jsonPath("$.error.message").value("Data source not in scope"))
            .andExpect(jsonPath("$.data").doesNotExist());
    }
}
