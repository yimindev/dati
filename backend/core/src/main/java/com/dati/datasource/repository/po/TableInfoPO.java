package com.dati.datasource.repository.po;

import com.dati.base.pojo.BaseResourcePO;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@FieldNameConstants
@Entity
@Table(name = "table_info")
public class TableInfoPO extends BaseResourcePO {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(length = 36)
    private String dataSourceId;

    @Column(name = "table_schema", length = 64)
    private String schema;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private List<String> aliases = new ArrayList<>();

}
