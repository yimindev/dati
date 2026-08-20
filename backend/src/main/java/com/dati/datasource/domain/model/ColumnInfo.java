package com.dati.datasource.domain.model;

import com.dati.base.pojo.BaseResource;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ColumnInfo extends BaseResource {

    private String tableId;

    private String columnType;

    private List<String> aliases;

    private Boolean extractValueEnabled;

}
