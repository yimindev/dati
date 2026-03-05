package com.dati.datasource.repository.po;

import com.dati.base.pojo.BaseResourcePO;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

@Getter
@Setter
@Entity
@Table(name = "column_info")
public class ColumnInfoPO extends BaseResourcePO {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(length = 36)
    private String tableId;

    @Column(length = 64)
    private String columnName;

    @Column(length = 64)
    private String columnType;

    @Column(columnDefinition = "TEXT")
    private String comment;

}
