package com.dati.datasource.server.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A table already imported into a data source, identified by schema and name. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddedTableVO {
    private String schema;
    private String name;
}
