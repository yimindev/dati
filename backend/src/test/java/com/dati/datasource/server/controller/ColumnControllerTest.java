package com.dati.datasource.server.controller;

import com.dati.TestFixtures;
import com.dati.base.pojo.PageReq;
import com.dati.base.pojo.PageResponse;
import com.dati.datasource.domain.model.ColumnInfo;
import com.dati.datasource.domain.service.ColumnService;
import com.dati.datasource.domain.service.ColumnValueService;
import com.dati.datasource.server.assembler.ColumnAssembler;
import com.dati.datasource.server.pojo.ColumnInfoVO;
import com.dati.datasource.server.pojo.ColumnValueVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ColumnController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ColumnController integration tests")
class ColumnControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ColumnService columnService;

    @MockitoBean
    private ColumnAssembler columnAssembler;

    @MockitoBean
    private ColumnValueService columnValueService;

    private ColumnInfo testColumnInfo;

    @BeforeEach
    void setUp() {
        testColumnInfo = TestFixtures.createTestColumnInfo();
    }

    @Test
    @DisplayName("Paged query columns - success")
    void getColumns_shouldReturnPagedResults() throws Exception {
        // given
        Page<ColumnInfo> page = new PageImpl<>(List.of(testColumnInfo));
        when(columnService.getColumns(any(), eq(TestFixtures.TEST_TABLE_ID), any()))
            .thenReturn(page);

        ColumnInfoVO vo = new ColumnInfoVO();
        vo.setId(TestFixtures.TEST_COLUMN_ID);
        vo.setName("test_column");
        vo.setTableId(TestFixtures.TEST_TABLE_ID);
        vo.setColumnType("VARCHAR(255)");
        PageResponse<ColumnInfoVO> pageResponse = PageResponse.of(
                new PageImpl<>(List.of(vo), page.getPageable(), page.getTotalElements()));
        when(columnAssembler.toPageResponse(page)).thenReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/v1/data-sources/{datasourceId}/tables/{tableId}/columns",
                    TestFixtures.TEST_DATASOURCE_ID, TestFixtures.TEST_TABLE_ID)
                .param("page", "1")
                .param("page_size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data[0].id").value(TestFixtures.TEST_COLUMN_ID))
            .andExpect(jsonPath("$.data[0].table_id").value(TestFixtures.TEST_TABLE_ID))
            .andExpect(jsonPath("$.data[0].column_type").value("VARCHAR(255)"));
    }

    @Test
    @DisplayName("Paged query columns - with keyword")
    void getColumns_withKeyword_shouldReturnFilteredResults() throws Exception {
        // given
        Page<ColumnInfo> page = new PageImpl<>(List.of(testColumnInfo));
        when(columnService.getColumns(any(), eq(TestFixtures.TEST_TABLE_ID), eq("id")))
            .thenReturn(page);

        ColumnInfoVO vo = new ColumnInfoVO();
        vo.setId(TestFixtures.TEST_COLUMN_ID);
        vo.setName("user_id");
        PageResponse<ColumnInfoVO> pageResponse = PageResponse.of(
                new PageImpl<>(List.of(vo), page.getPageable(), page.getTotalElements()));
        when(columnAssembler.toPageResponse(page)).thenReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/v1/data-sources/{datasourceId}/tables/{tableId}/columns",
                    TestFixtures.TEST_DATASOURCE_ID, TestFixtures.TEST_TABLE_ID)
                .param("page", "1")
                .param("page_size", "10")
                .param("keyword", "id"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("Update column - success")
    void updateColumn_shouldReturnId() throws Exception {
        // given
        ColumnInfoVO requestVo = new ColumnInfoVO();
        requestVo.setName("updated_column");
        requestVo.setColumnType("INTEGER");
        requestVo.setTableId(TestFixtures.TEST_TABLE_ID);

        ColumnInfo columnInfo = TestFixtures.createTestColumnInfo();
        columnInfo.setName("updated_column");

        when(columnAssembler.toColumnInfo(any())).thenReturn(columnInfo);
        doNothing().when(columnService).updateColumn(eq(TestFixtures.TEST_COLUMN_ID), any());

        // when & then
        mockMvc.perform(put("/v1/data-sources/{datasourceId}/tables/{tableId}/columns/{id}",
                    TestFixtures.TEST_DATASOURCE_ID, TestFixtures.TEST_TABLE_ID, TestFixtures.TEST_COLUMN_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestVo)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(TestFixtures.TEST_COLUMN_ID));

        verify(columnService).updateColumn(eq(TestFixtures.TEST_COLUMN_ID), any());
    }

    @Test
    @DisplayName("Sync columns - success (no overwrite by default)")
    void syncColumns_shouldReturnTableId() throws Exception {
        // given
        doNothing().when(columnService).syncColumns(TestFixtures.TEST_DATASOURCE_ID, TestFixtures.TEST_TABLE_ID, false);

        // when & then
        mockMvc.perform(post("/v1/data-sources/{datasourceId}/tables/{tableId}/columns/sync",
                    TestFixtures.TEST_DATASOURCE_ID, TestFixtures.TEST_TABLE_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(TestFixtures.TEST_TABLE_ID));

        verify(columnService).syncColumns(TestFixtures.TEST_DATASOURCE_ID, TestFixtures.TEST_TABLE_ID, false);
    }

    @Test
    @DisplayName("Sync columns - explicit overwrite of existing content")
    void syncColumns_withOverwriteExistingTrue_shouldPassTrueToService() throws Exception {
        // given
        doNothing().when(columnService).syncColumns(TestFixtures.TEST_DATASOURCE_ID, TestFixtures.TEST_TABLE_ID, true);

        // when & then
        mockMvc.perform(post("/v1/data-sources/{datasourceId}/tables/{tableId}/columns/sync",
                    TestFixtures.TEST_DATASOURCE_ID, TestFixtures.TEST_TABLE_ID)
                .param("overwrite_existing", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(TestFixtures.TEST_TABLE_ID));

        verify(columnService).syncColumns(TestFixtures.TEST_DATASOURCE_ID, TestFixtures.TEST_TABLE_ID, true);
    }

    @Test
    @DisplayName("Update column - verifies ID param is passed")
    void updateColumn_shouldPassCorrectId() throws Exception {
        // given
        ColumnInfoVO requestVo = new ColumnInfoVO();
        requestVo.setName("new_name");

        when(columnAssembler.toColumnInfo(any())).thenReturn(testColumnInfo);
        doNothing().when(columnService).updateColumn(anyString(), any());

        // when & then
        mockMvc.perform(put("/v1/data-sources/{datasourceId}/tables/{tableId}/columns/{id}",
                    TestFixtures.TEST_DATASOURCE_ID, TestFixtures.TEST_TABLE_ID, "specific-column-id")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestVo)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("specific-column-id"));

        verify(columnService).updateColumn(eq("specific-column-id"), any());
    }

    @Test
    @DisplayName("Paged query columns - verifies path params are passed")
    void getColumns_shouldPassCorrectPathParams() throws Exception {
        // given
        Page<ColumnInfo> page = new PageImpl<>(List.of());
        when(columnService.getColumns(any(), eq(TestFixtures.TEST_TABLE_ID), isNull()))
            .thenReturn(page);

        // when & then
        mockMvc.perform(get("/v1/data-sources/{datasourceId}/tables/{tableId}/columns",
                    TestFixtures.TEST_DATASOURCE_ID, TestFixtures.TEST_TABLE_ID)
                .param("page", "1")
                .param("page_size", "10"))
            .andExpect(status().isOk());

        verify(columnService).getColumns(any(), eq(TestFixtures.TEST_TABLE_ID), isNull());
    }

    @Test
    @DisplayName("Paged query column values - success")
    void getValues_shouldReturnPagedResults() throws Exception {
        // given
        ColumnValueVO vo = new ColumnValueVO();
        vo.setId("doc1");
        vo.setValue("北京");
        vo.setSynonyms(List.of("帝都"));

        Page<ColumnValueService.ValueItem> page = new PageImpl<>(
                List.of(createValueItem(List.of("帝都"))),
                PageRequest.of(0, 10),
                1
        );
        when(columnValueService.getValues(anyString(), any(PageReq.class), isNull())).thenReturn(page);

        // when & then
        mockMvc.perform(get("/v1/data-sources/{dsId}/tables/{tableId}/columns/{columnId}/values",
                    TestFixtures.TEST_DATASOURCE_ID, TestFixtures.TEST_TABLE_ID, TestFixtures.TEST_COLUMN_ID)
                .param("page", "1")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data[0].id").value("doc1"))
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.size").value(10));
    }

    private ColumnValueService.ValueItem createValueItem(List<String> synonyms) {
        ColumnValueService.ValueItem item = new ColumnValueService.ValueItem();
        item.setId("doc1");
        item.setValue("北京");
        item.setSynonyms(synonyms);
        return item;
    }
}
