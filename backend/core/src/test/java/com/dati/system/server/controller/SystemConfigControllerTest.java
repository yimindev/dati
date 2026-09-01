package com.dati.system.server.controller;

import com.dati.config.ColumnValueConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SystemConfigController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("SystemConfigController unit tests")
class SystemConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ColumnValueConfig columnValueConfig;

    @Test
    @DisplayName("Should return system config with supported database types")
    void getConfig_shouldReturnSystemConfigWithSupportedDatabaseTypes() throws Exception {
        when(columnValueConfig.getColumnValueSampleLimit()).thenReturn(1000);
        when(columnValueConfig.getColumnValueLengthLimit()).thenReturn(256);

        mockMvc.perform(get("/v1/system/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.column_value_sample_limit").value(1000))
                .andExpect(jsonPath("$.column_value_length_limit").value(256))
                .andExpect(jsonPath("$.supported_database_types").isArray())
                .andExpect(jsonPath("$.supported_database_types.length()").value(5))
                .andExpect(jsonPath("$.supported_database_types[0].type").value("MYSQL"))
                .andExpect(jsonPath("$.supported_database_types[0].label").value("MySQL"))
                .andExpect(jsonPath("$.supported_database_types[0].default_port").value(3306))
                .andExpect(jsonPath("$.supported_database_types[0].jdbc_url_template").isNotEmpty())
                .andExpect(jsonPath("$.supported_database_types[1].type").value("POSTGRESQL"))
                .andExpect(jsonPath("$.supported_database_types[1].label").value("PostgreSQL"))
                .andExpect(jsonPath("$.supported_database_types[1].default_port").value(5432))
                .andExpect(jsonPath("$.supported_database_types[2].type").value("MARIADB"))
                .andExpect(jsonPath("$.supported_database_types[3].type").value("CLICKHOUSE"))
                .andExpect(jsonPath("$.supported_database_types[4].type").value("DORIS"));
    }
}
