package com.dati.datasource.repository.po;

import com.dati.base.pojo.BaseResourcePO;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@FieldNameConstants
@Entity
@Table(name = "column_info")
public class ColumnInfoPO extends BaseResourcePO {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(length = 36)
    private String tableId;

    @Column(length = 64)
    private String columnType;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> aliases = new ArrayList<>();

    @ColumnDefault( "false")
    private boolean extractValueEnabled = false;

}
