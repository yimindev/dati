package com.dati.mcp.server.controller;

import com.dati.TestFixtures;
import com.dati.mcp.domain.model.McpService;
import com.dati.mcp.domain.service.McpServiceService;
import com.dati.mcp.server.assembler.McpServiceAssembler;
import com.dati.mcp.server.pojo.McpServiceVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(McpServiceController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("McpServiceController 集成测试")
class McpServiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private McpServiceService mcpServiceService;

    @MockitoBean
    private McpServiceAssembler mcpServiceAssembler;

    private McpService testService;

    @BeforeEach
    void setUp() {
        testService = TestFixtures.createTestMcpService();
    }

    @Test
    @DisplayName("创建 MCP 服务 - 成功")
    void createMcpService_shouldReturnId() throws Exception {
        // given
        when(mcpServiceService.createMcpService(any())).thenReturn(TestFixtures.TEST_MCP_SERVICE_ID);
        doNothing().when(mcpServiceAssembler).fillUsersFromRequest(any());

        // when & then
        mockMvc.perform(post("/v1/mcp-services")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testService)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(TestFixtures.TEST_MCP_SERVICE_ID));
    }

    @Test
    @DisplayName("更新 MCP 服务 - 成功")
    void updateMcpService_shouldReturnId() throws Exception {
        // given
        doNothing().when(mcpServiceAssembler).fillUpdateUserFromRequest(any());
        doNothing().when(mcpServiceService).updateMcpService(anyString(), any());

        // when & then
        mockMvc.perform(put("/v1/mcp-services/{id}", TestFixtures.TEST_MCP_SERVICE_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testService)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(TestFixtures.TEST_MCP_SERVICE_ID));
    }

    @Test
    @DisplayName("查询 MCP 服务详情 - 成功")
    void getMcpService_shouldReturnDetail() throws Exception {
        // given
        when(mcpServiceService.getMcpService(TestFixtures.TEST_MCP_SERVICE_ID)).thenReturn(testService);

        McpServiceVO vo = new McpServiceVO();
        vo.setId(TestFixtures.TEST_MCP_SERVICE_ID);
        vo.setName("Test MCP Service");
        vo.setStatus("DRAFT");
        vo.setEndpointPath("/" + TestFixtures.TEST_MCP_SERVICE_ID + "/mcp");
        when(mcpServiceAssembler.toMcpServiceVO(any())).thenReturn(vo);

        // when & then
        mockMvc.perform(get("/v1/mcp-services/{id}", TestFixtures.TEST_MCP_SERVICE_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(TestFixtures.TEST_MCP_SERVICE_ID))
            .andExpect(jsonPath("$.name").value("Test MCP Service"))
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.endpoint_path").value("/" + TestFixtures.TEST_MCP_SERVICE_ID + "/mcp"));
    }

    @Test
    @DisplayName("分页查询 MCP 服务 - 成功")
    void listMcpServices_shouldReturnPagedResults() throws Exception {
        // given
        Page<McpService> page = new PageImpl<>(List.of(testService));
        when(mcpServiceService.listMcpServices(isNull(), isNull(), any())).thenReturn(page);

        McpServiceVO vo = new McpServiceVO();
        vo.setId(TestFixtures.TEST_MCP_SERVICE_ID);
        vo.setName("Test MCP Service");
        when(mcpServiceAssembler.toMcpServiceVO(any())).thenReturn(vo);

        // when & then
        mockMvc.perform(get("/v1/mcp-services")
                .param("page", "1")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data[0].id").value(TestFixtures.TEST_MCP_SERVICE_ID));
    }

    @Test
    @DisplayName("分页查询 MCP 服务 - 带关键词和状态")
    void listMcpServices_withKeywordAndStatus() throws Exception {
        // given
        Page<McpService> page = new PageImpl<>(List.of(testService));
        when(mcpServiceService.listMcpServices(any(), any(), any())).thenReturn(page);

        McpServiceVO vo = new McpServiceVO();
        vo.setId(TestFixtures.TEST_MCP_SERVICE_ID);
        vo.setName("Test MCP Service");
        when(mcpServiceAssembler.toMcpServiceVO(any())).thenReturn(vo);

        // when & then
        mockMvc.perform(get("/v1/mcp-services")
                .param("page", "1")
                .param("size", "10")
                .param("keyword", "test")
                .param("status", "DRAFT"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data[0].id").value(TestFixtures.TEST_MCP_SERVICE_ID));
    }
}
