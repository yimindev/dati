package com.dati.datasource.server.controller;

import com.dati.TestFixtures;
import com.dati.base.pojo.PageResponse;
import com.dati.datasource.domain.model.DataSource;
import com.dati.datasource.domain.service.DataSourceService;
import com.dati.datasource.domain.service.JdbcMetaService;
import com.dati.datasource.server.assembler.DSAssembler;
import com.dati.datasource.server.pojo.DatasourceVO;
import com.dati.db.Column;
import com.dati.db.Table;
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
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DataSourceController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("DataSourceController integration tests")
class DataSourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DataSourceService dataSourceService;

    @MockitoBean
    private JdbcMetaService jdbcMetaService;

    @MockitoBean
    private DSAssembler dsAssembler;

    private DataSource testDataSource;

    @BeforeEach
    void setUp() {
        testDataSource = TestFixtures.createTestDataSource();
    }

    @Test
    @DisplayName("Test connection - success")
    void testConnection_shouldReturnTrue() throws Exception {
        // given
        when(dataSourceService.testConnection(any())).thenReturn(true);

        // when & then
        mockMvc.perform(post("/v1/data-sources/test-connection")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testDataSource)))
            .andExpect(status().isOk())
            .andExpect(content().string("true"));
    }

    @Test
    @DisplayName("Test connection - failure")
    void testConnection_shouldReturnFalse() throws Exception {
        // given
        when(dataSourceService.testConnection(any())).thenReturn(false);

        // when & then
        mockMvc.perform(post("/v1/data-sources/test-connection")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testDataSource)))
            .andExpect(status().isOk())
            .andExpect(content().string("false"));
    }

    @Test
    @DisplayName("Add data source - success")
    void addDataSource_shouldReturnId() throws Exception {
        // given
        when(dataSourceService.addDataSource(any())).thenReturn("new-ds-id");
        doNothing().when(dsAssembler).fillUsersFromRequest(any());

        // when & then
        mockMvc.perform(post("/v1/data-sources")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testDataSource)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("new-ds-id"));
    }

    @Test
    @DisplayName("Update data source - success")
    void updateDataSource_shouldReturnId() throws Exception {
        // given
        doNothing().when(dsAssembler).fillUpdateUserFromRequest(any());
        doNothing().when(dataSourceService).updateDataSource(anyString(), any());

        // when & then
        mockMvc.perform(put("/v1/data-sources/{id}", TestFixtures.TEST_DATASOURCE_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testDataSource)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @DisplayName("Update data source - partial update with only name should succeed")
    void updateDataSource_partialUpdate_shouldNotRequireAllFields() throws Exception {
        // given
        doNothing().when(dsAssembler).fillUpdateUserFromRequest(any());
        doNothing().when(dataSourceService).updateDataSource(anyString(), any());

        // when & then: PUT with only name (no type/jdbcUrl/username) must not be blocked by @Valid
        mockMvc.perform(put("/v1/data-sources/{id}", TestFixtures.TEST_DATASOURCE_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"renamed\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @DisplayName("Delete data source - success")
    void deleteDataSource_shouldReturnId() throws Exception {
        // given
        doNothing().when(dataSourceService).deleteDataSource(TestFixtures.TEST_DATASOURCE_ID);

        // when & then
        mockMvc.perform(delete("/v1/data-sources/{id}", TestFixtures.TEST_DATASOURCE_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(TestFixtures.TEST_DATASOURCE_ID));
    }

    @Test
    @DisplayName("Paged query data sources - success")
    void listDataSources_shouldReturnPagedResults() throws Exception {
        // given
        Page<DataSource> page = new PageImpl<>(List.of(testDataSource));
        when(dataSourceService.listDataSources(any(), any())).thenReturn(page);

        DatasourceVO vo = new DatasourceVO();
        vo.setId(TestFixtures.TEST_DATASOURCE_ID);
        vo.setName("Test MySQL DataSource");
        PageResponse<DatasourceVO> pageResponse = PageResponse.of(new PageImpl<>(List.of(vo)));
        when(dsAssembler.toPageResponse(any())).thenReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/v1/data-sources")
                .param("page", "1")
                .param("page_size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data[0].id").value(TestFixtures.TEST_DATASOURCE_ID));
    }

    @Test
    @DisplayName("Get schemas - success")
    void getSchemas_shouldReturnSchemaList() throws Exception {
        // given
        when(jdbcMetaService.getSchemas(TestFixtures.TEST_DATASOURCE_ID, null))
            .thenReturn(List.of("public", "information_schema"));

        // when & then
        mockMvc.perform(get("/v1/data-sources/{id}/schemas", TestFixtures.TEST_DATASOURCE_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0]").value("public"));
    }

    @Test
    @DisplayName("Get tables - success")
    void getTables_shouldReturnTableList() throws Exception {
        // given
        when(jdbcMetaService.getTables(TestFixtures.TEST_DATASOURCE_ID, null, "public"))
            .thenReturn(List.of(new Table("users", "用户表"), new Table("orders", "订单表"), new Table("products", "产品表")));

        // when & then
        mockMvc.perform(get("/v1/data-sources/{id}/schemas/{schema}/tables", 
                    TestFixtures.TEST_DATASOURCE_ID, "public"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$", hasSize(3)))
            .andExpect(jsonPath("$[0].name").value("users"))
            .andExpect(jsonPath("$[0].comment").value("用户表"));
    }

    @Test
    @DisplayName("Get columns - success")
    void getColumns_shouldReturnColumnList() throws Exception {
        // given
        Column column = new Column("id", "INTEGER", "Primary key");
        when(jdbcMetaService.getColumns(TestFixtures.TEST_DATASOURCE_ID, null, "public", "users"))
            .thenReturn(List.of(column));

        // when & then
        mockMvc.perform(get("/v1/data-sources/{id}/schemas/{schema}/tables/{table}/columns",
                    TestFixtures.TEST_DATASOURCE_ID, "public", "users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].name").value("id"))
            .andExpect(jsonPath("$[0].type").value("INTEGER"));
    }

    @Test
    @DisplayName("Get single data source - returns default_schema")
    void getDataSource_shouldReturnWithDefaultSchema() throws Exception {
        // given
        when(dataSourceService.getDataSource(TestFixtures.TEST_DATASOURCE_ID)).thenReturn(Optional.of(testDataSource));

        DatasourceVO vo = new DatasourceVO();
        vo.setId(TestFixtures.TEST_DATASOURCE_ID);
        vo.setName("Test MySQL DataSource");
        vo.setDefaultSchema("public");
        when(dsAssembler.toDatasourceVO(any(DataSource.class))).thenReturn(vo);

        // when & then
        mockMvc.perform(get("/v1/data-sources/" + TestFixtures.TEST_DATASOURCE_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(TestFixtures.TEST_DATASOURCE_ID))
            .andExpect(jsonPath("$.default_schema").value("public"));
    }

    @Test
    @DisplayName("Execute SQL - success")
    void executeSql_shouldReturnResults() throws Exception {
        // given
        Map<String, Object> row = Map.of("id", 1, "name", "test");
        when(jdbcMetaService.executeSql(TestFixtures.TEST_DATASOURCE_ID, "SELECT * FROM users"))
            .thenReturn(List.of(row));

        String requestBody = "{\"sql\": \"SELECT * FROM users\"}";

        // when & then
        mockMvc.perform(post("/v1/data-sources/{id}/execute-sql", TestFixtures.TEST_DATASOURCE_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].name").value("test"));
    }

}
