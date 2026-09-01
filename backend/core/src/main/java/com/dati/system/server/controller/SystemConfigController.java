package com.dati.system.server.controller;

import com.dati.config.ColumnValueConfig;
import com.dati.datasource.server.pojo.DatabaseTypeVO;
import com.dati.db.DbType;
import com.dati.system.server.pojo.SystemConfigResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/v1/system")
public class SystemConfigController {

    private final ColumnValueConfig columnValueConfig;

    public SystemConfigController(ColumnValueConfig columnValueConfig) {
        this.columnValueConfig = columnValueConfig;
    }

    @GetMapping("/config")
    public SystemConfigResponse getConfig() {
        List<DatabaseTypeVO> supportedDatabaseTypes = Arrays.stream(DbType.values())
                .filter(DbType::isSupported)
                .map(type -> DatabaseTypeVO.builder()
                        .type(type)
                        .label(type.getLabel())
                        .defaultPort(type.getDefaultPort())
                        .jdbcUrlTemplate(type.getJdbcUrlTemplate())
                        .build())
                .toList();

        return SystemConfigResponse.builder()
                .columnValueSampleLimit(columnValueConfig.getColumnValueSampleLimit())
                .columnValueLengthLimit(columnValueConfig.getColumnValueLengthLimit())
                .supportedDatabaseTypes(supportedDatabaseTypes)
                .build();
    }
}

