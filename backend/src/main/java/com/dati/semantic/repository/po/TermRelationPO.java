package com.dati.semantic.repository.po;

import com.dati.base.pojo.BasePO;
import com.dati.semantic.domain.SemanticEntityType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import java.io.Serial;

@Setter
@Getter
@FieldNameConstants
@Entity
@Table(name = "term_relation")
public class TermRelationPO extends BasePO {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "term_id", nullable = false, length = 64)
    private String termId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 32)
    private SemanticEntityType entityType;

    @Column(name = "table_id", nullable = false, length = 64)
    private String tableId;

    @Column(name = "field_name", length = 128)
    private String fieldName;
}