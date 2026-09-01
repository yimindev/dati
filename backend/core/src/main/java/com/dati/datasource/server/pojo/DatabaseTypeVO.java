package com.dati.datasource.server.pojo;

import com.dati.db.DbType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseTypeVO {
    private DbType type;
    private String label;
    private Integer defaultPort;
    private String jdbcUrlTemplate;
}
