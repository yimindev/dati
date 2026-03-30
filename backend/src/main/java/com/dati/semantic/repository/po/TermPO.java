package com.dati.semantic.repository.po;

import com.dati.base.pojo.BaseResourcePO;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;

@Setter
@Getter
@Entity
@Table(name = "term")
public class TermPO extends BaseResourcePO {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "subject_id", nullable = false, length = 64)
    private String subjectId;
}