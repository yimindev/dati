package com.dati.datasource.repository.po;

import com.dati.base.pojo.BaseResourcePO;
import com.dati.db.DbType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import java.io.Serial;

@Setter
@Getter
@FieldNameConstants
@Entity
@Table(name = "data_source")
public class DataSourcePO extends BaseResourcePO {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(length = 256)
    private String jdbcUrl;

    @Enumerated(EnumType.STRING)
    private DbType type;

    @Column(length = 64)
    private String userName;

    @Column(length = 512)
    private String encryptedPassword;

}
