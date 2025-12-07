package com.dati.base.pojo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

@Getter
@Setter
@MappedSuperclass
@FieldNameConstants
public class BaseResourcePO extends BasePO {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(length = 64)
    private String name;

    @Column(length = 256)
    private String description;

    @Column(columnDefinition = "boolean default false")
    private Boolean deleted = false;
}
