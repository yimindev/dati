package com.dati.datasource.server.pojo.request;

import com.dati.db.DbType;
import lombok.Data;

/**
 * Request body for partial update of a data source.
 * All fields are optional; null fields fall back to current values in the service layer.
 */
@Data
public class UpdateDataSourceRequest {
    private String name;
    private String description;
    private DbType type;
    private String jdbcUrl;
    private String username;
    private String password;
    private String defaultSchema;
}
