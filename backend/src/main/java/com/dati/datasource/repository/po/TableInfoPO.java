package com.dati.datasource.repository.po;

import com.dati.base.pojo.BaseResourcePO;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

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

}
