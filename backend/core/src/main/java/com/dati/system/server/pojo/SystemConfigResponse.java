package com.dati.system.server.pojo;

import com.dati.datasource.server.pojo.DatabaseTypeVO;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SystemConfigResponse {
    private Integer columnValueSampleLimit;
    private Integer columnValueLengthLimit;
    private List<DatabaseTypeVO> supportedDatabaseTypes;
}

