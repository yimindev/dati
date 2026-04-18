package com.dati.system.server.controller;

import com.dati.config.ColumnValueConfig;
import com.dati.system.server.pojo.SystemConfigResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/system")
public class SystemConfigController {

    private final ColumnValueConfig columnValueConfig;

    public SystemConfigController(ColumnValueConfig columnValueConfig) {
        this.columnValueConfig = columnValueConfig;
    }

    @GetMapping("/config")
    public SystemConfigResponse getConfig() {
        return SystemConfigResponse.builder()
                .columnValueSampleLimit(columnValueConfig.getColumnValueSampleLimit())
                .columnValueLengthLimit(columnValueConfig.getColumnValueLengthLimit())
                .build();
    }
}
