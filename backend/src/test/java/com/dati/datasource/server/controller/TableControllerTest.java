package com.dati.datasource.server.controller;

import com.dati.TestFixtures;
import com.dati.base.pojo.PageResponse;
import com.dati.datasource.domain.model.TableInfo;
import com.dati.datasource.domain.service.TableService;
import com.dati.datasource.server.assembler.TableAssembler;
import com.dati.datasource.server.pojo.AddTableRequest;
import com.dati.datasource.server.pojo.TableInfoVO;
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

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TableController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("TableController 集成测试")
class TableControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TableService tableService;

    @MockitoBean
    private TableAssembler tableAssembler;

    private TableInfo testTableInfo;

    @BeforeEach
    void setUp() {
        testTableInfo = TestFixtures.createTestTableInfo();
    }

    @Test
    @DisplayName("分页查询表 - 成功")
    void getTables_shouldReturnPagedResults() throws Exception {
        // given
        Page<TableInfo> page = new PageImpl<>(List.of(testTableInfo));
        when(tableService.getTables(any(), eq(TestFixtures.TEST_DATASOURCE_ID), any()))
            .thenReturn(page);

        TableInfoVO vo = new TableInfoVO();
        vo.setId(TestFixtures.TEST_TABLE_ID);
        vo.setName("test_table");
        vo.setDatasourceId(TestFixtures.TEST_DATASOURCE_ID);
        PageResponse<TableInfoVO> pageResponse = PageResponse.of(
                new PageImpl<>(List.of(vo), page.getPageable(), page.getTotalElements()));
        when(tableAssembler.toPageResponse(page)).thenReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/v1/data-sources/{datasourceId}/tables", TestFixtures.TEST_DATASOURCE_ID)
                .param("page", "1")
                .param("page_size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data[0].id").value(TestFixtures.TEST_TABLE_ID))
            .andExpect(jsonPath("$.data[0].datasource_id").value(TestFixtures.TEST_DATASOURCE_ID));
    }

    @Test
    @DisplayName("分页查询表 - 带关键词")
    void getTables_withKeyword_shouldReturnFilteredResults() throws Exception {
        // given
        Page<TableInfo> page = new PageImpl<>(List.of(testTableInfo));
        when(tableService.getTables(any(), eq(TestFixtures.TEST_DATASOURCE_ID), eq("user")))
            .thenReturn(page);

        TableInfoVO vo = new TableInfoVO();
        vo.setId(TestFixtures.TEST_TABLE_ID);
        vo.setName("users");
        PageResponse<TableInfoVO> pageResponse = PageResponse.of(
                new PageImpl<>(List.of(vo), page.getPageable(), page.getTotalElements()));
        when(tableAssembler.toPageResponse(page)).thenReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/v1/data-sources/{datasourceId}/tables", TestFixtures.TEST_DATASOURCE_ID)
                .param("page", "1")
                .param("page_size", "10")
                .param("keyword", "user"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("获取已添加的表名列表 - 成功")
    void getAddedTableNames_shouldReturnTableNames() throws Exception {
        // given
        when(tableService.getAddedTableNames(TestFixtures.TEST_DATASOURCE_ID))
            .thenReturn(List.of("users", "orders", "products"));

        // when & then
        mockMvc.perform(get("/v1/data-sources/{datasourceId}/tables/added-names", TestFixtures.TEST_DATASOURCE_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$", hasSize(3)))
            .andExpect(jsonPath("$[0]").value("users"));
    }

    @Test
    @DisplayName("批量添加表 - 成功")
    void batchAddTables_shouldReturnCount() throws Exception {
        // given
        List<AddTableRequest> requests = List.of(
            createAddTableRequest("users"),
            createAddTableRequest("orders")
        );
        
        when(tableService.batchAddTables(eq(TestFixtures.TEST_DATASOURCE_ID), any()))
            .thenReturn(List.of("table-1", "table-2"));

        // when & then
        mockMvc.perform(post("/v1/data-sources/{datasourceId}/tables/batch", TestFixtures.TEST_DATASOURCE_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requests)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("2"));
    }

    @Test
    @DisplayName("批量添加表 - 空列表")
    void batchAddTables_withEmptyList_shouldReturnZero() throws Exception {
        // given
        when(tableService.batchAddTables(eq(TestFixtures.TEST_DATASOURCE_ID), any()))
            .thenReturn(List.of());

        // when & then
        mockMvc.perform(post("/v1/data-sources/{datasourceId}/tables/batch", TestFixtures.TEST_DATASOURCE_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("[]"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("0"));
    }

    @Test
    @DisplayName("删除表 - 成功")
    void deleteTable_shouldReturnOk() throws Exception {
        // given
        doNothing().when(tableService).deleteTable(TestFixtures.TEST_TABLE_ID);

        // when & then
        mockMvc.perform(delete("/v1/data-sources/{datasourceId}/tables/{tableId}", 
                    TestFixtures.TEST_DATASOURCE_ID, TestFixtures.TEST_TABLE_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(TestFixtures.TEST_TABLE_ID));
        
        verify(tableService).deleteTable(TestFixtures.TEST_TABLE_ID);
    }

    @Test
    @DisplayName("删除表 - 验证datasourceId参数传递但不影响删除逻辑")
    void deleteTable_shouldIgnoreDatasourceId() throws Exception {
        // given
        doNothing().when(tableService).deleteTable(TestFixtures.TEST_TABLE_ID);

        // when & then
        mockMvc.perform(delete("/v1/data-sources/{datasourceId}/tables/{tableId}", 
                    "any-datasource-id", TestFixtures.TEST_TABLE_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(TestFixtures.TEST_TABLE_ID));
        
        // Verify that only tableId is used for deletion
        verify(tableService).deleteTable(TestFixtures.TEST_TABLE_ID);
        verify(tableService, never()).getAddedTableNames(any());
    }

    private AddTableRequest createAddTableRequest(String name) {
        AddTableRequest request = new AddTableRequest();
        request.setName(name);
        request.setSchema("public");
        return request;
    }
}
