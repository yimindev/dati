package com.dati.datasource.server.pojo.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Partial update of column metadata. The physical column name is not editable:
 * it is owned by the database and changing it would break term relations and
 * indexed sample values. Every field is optional; omitted fields keep their value.
 */
@Data
public class UpdateColumnRequest {

    @Size(max = 1024)
    private String description;

    @Size(max = 64)
    private String columnType;

    private List<String> aliases;

    private Boolean extractValueEnabled;

}
