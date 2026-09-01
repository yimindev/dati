package com.dati.semantic.repository.po;

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

@Setter
@Getter
@FieldNameConstants
@Entity
@Table(name = "subject")
public class SubjectPO extends BaseResourcePO {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "datasource_id", nullable = false, length = 64)
    private String datasourceId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private List<String> aliases = new ArrayList<>();
}