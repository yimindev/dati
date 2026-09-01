package com.dati.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "system")
public class ColumnValueConfig {

    private Integer columnValueSampleLimit = 1000;

    private Integer columnValueLengthLimit = 256;
}
