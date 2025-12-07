package com.dati.datasource.repository.po;

import com.dati.base.pojo.BaseResourcePO;
import com.dati.datasource.domain.model.ColumnDef;
import com.dati.datasource.repository.converter.ColumnListConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "table_info")
public class TableInfoPO extends BaseResourcePO {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(length = 36)
    private String dataSourceId;

    @Column(length = 64)
    private String schema;

    @Column(length = 64)
    private String displayName;

    @Lob
    @Convert(converter = ColumnListConverter.class)
    private List<ColumnDef> columns;

}
