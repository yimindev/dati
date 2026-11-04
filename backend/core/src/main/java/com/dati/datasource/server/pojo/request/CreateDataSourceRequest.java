package com.dati.datasource.server.pojo.request;

import com.dati.db.DbType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for creating a data source. Length limits mirror the storage column widths.
 */
@Data
public class CreateDataSourceRequest {

    @NotBlank
    @Size(max = 64)
    private String name;

    @Size(max = 1024)
    private String description;

    @NotNull
    private DbType type;

    @NotBlank
    @Size(max = 256)
    private String jdbcUrl;

    @NotBlank
    @Size(max = 64)
    private String username;

    @Size(max = 512)
    private String password;

}
