package com.dati.semantic.repository.po;

import com.dati.base.pojo.BaseResourcePO;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

@Setter
@Getter
@Entity
@Table(name = "subject")
public class SubjectPO extends BaseResourcePO {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "datasource_id", nullable = false, length = 64)
    private String datasourceId;
}