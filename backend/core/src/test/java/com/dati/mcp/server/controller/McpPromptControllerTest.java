package com.dati.mcp.server.controller;

import com.dati.TestFixtures;
import com.dati.mcp.domain.model.McpPrompt;
import com.dati.mcp.domain.service.McpPromptService;
import com.dati.mcp.server.assembler.McpPromptAssembler;
import com.dati.mcp.server.pojo.McpPromptRequest;
import com.dati.mcp.server.pojo.McpPromptVO;
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

@WebMvcTest(McpPromptController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("McpPromptController integration tests")
class McpPromptControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private McpPromptService promptService;
    @MockitoBean private McpPromptAssembler promptAssembler;

    @BeforeEach
    void setUp() {
        McpPromptVO vo = new McpPromptVO();
        vo.setId(TestFixtures.TEST_MCP_PROMPT_ID);
        vo.setName("analyze_table");
        when(promptService.listPrompts(TestFixtures.TEST_MCP_SERVICE_ID))
            .thenReturn(List.of(TestFixtures.createTestMcpPrompt()));
        when(promptAssembler.toVOList(any())).thenReturn(List.of(vo));
    }

    @Test @DisplayName("GET /prompts - list")
    void list() throws Exception {
        mockMvc.perform(get("/v1/mcp-services/{id}/prompts", TestFixtures.TEST_MCP_SERVICE_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(TestFixtures.TEST_MCP_PROMPT_ID));
    }

    @Test @DisplayName("POST /prompts - create")
    void create() throws Exception {
        when(promptService.createPrompt(eq(TestFixtures.TEST_MCP_SERVICE_ID), any()))
            .thenReturn(TestFixtures.TEST_MCP_PROMPT_ID);
        mockMvc.perform(post("/v1/mcp-services/{id}/prompts", TestFixtures.TEST_MCP_SERVICE_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"analyze_table\",\"content\":\"请分析 {{table}} 表的数据。\",\"parameters\":[{\"name\":\"table\",\"required\":true}]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(TestFixtures.TEST_MCP_PROMPT_ID));
    }

    @Test @DisplayName("PUT /prompts/{id} - update")
    void update() throws Exception {
        when(promptAssembler.toModel(any(McpPromptRequest.class))).thenReturn(new McpPrompt());
        doNothing().when(promptService).updatePrompt(any(), any());
        mockMvc.perform(put("/v1/mcp-services/{id}/prompts/{pid}", TestFixtures.TEST_MCP_SERVICE_ID, TestFixtures.TEST_MCP_PROMPT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\":false}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(TestFixtures.TEST_MCP_PROMPT_ID));
    }

    @Test @DisplayName("DELETE /prompts/{id} - delete")
    void deletePrompt() throws Exception {
        doNothing().when(promptService).deletePrompt(TestFixtures.TEST_MCP_SERVICE_ID, TestFixtures.TEST_MCP_PROMPT_ID);
        mockMvc.perform(delete("/v1/mcp-services/{id}/prompts/{pid}", TestFixtures.TEST_MCP_SERVICE_ID, TestFixtures.TEST_MCP_PROMPT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(TestFixtures.TEST_MCP_PROMPT_ID));
    }
}
