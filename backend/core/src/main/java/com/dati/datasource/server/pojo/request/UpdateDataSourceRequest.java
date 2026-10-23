package com.dati.datasource.server.pojo.request;

import com.dati.db.DbType;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for partial update of a data source.
 * All fields are optional; null fields fall back to current values in the service layer.
 */
@Data
public class UpdateDataSourceRequest {
    @Size(max = 64)
    @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank")
    private String name;

    @Size(max = 1024)
    private String description;
    private DbType type;
    private String jdbcUrl;
    private String username;
    private String password;
    private String defaultSchema;
}
