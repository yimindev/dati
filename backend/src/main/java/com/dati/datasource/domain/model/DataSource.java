package com.dati.datasource.domain.model;

import com.dati.base.pojo.BaseResource;
import com.dati.db.DbType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class DataSource extends BaseResource {

    @NotNull
    private DbType type;

    @NotBlank
    private String jdbcUrl;

    @NotBlank
    private String username;

    private String password;

}
