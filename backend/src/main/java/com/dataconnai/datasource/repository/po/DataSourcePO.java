package com.dataconnai.datasource.repository.po;

import com.dataconnai.base.pojo.BaseResourcePO;
import com.dataconnai.db.DbType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

@Setter
@Getter
@Entity
@Table(name = "data_source")
public class DataSourcePO extends BaseResourcePO {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "jdbc_url", length = 256)
    private String jdbcUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private DbType type;

    @Column(name = "user_name", length = 64)
    private String userName;

    @Column(name = "encrypted_password", length = 512)
    private String encryptedPassword;

}
