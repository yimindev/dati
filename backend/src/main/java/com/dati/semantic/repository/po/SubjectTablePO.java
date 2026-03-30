package com.dati.semantic.repository.po;

import com.dati.base.pojo.BaseResourcePO;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

@Setter
@Getter
@Entity
@Table(name = "subject_table")
public class SubjectTablePO extends BaseResourcePO {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "subject_id", nullable = false, length = 64)
    private String subjectId;

    @Column(name = "table_id", nullable = false, length = 64)
    private String tableId;
}