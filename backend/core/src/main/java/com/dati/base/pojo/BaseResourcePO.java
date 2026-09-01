package com.dati.base.pojo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Setter
@MappedSuperclass
@FieldNameConstants
public class BaseResourcePO extends BasePO {

    @Column(length = 64)
    private String name;

    @Column(length = 1024)
    private String description;

    @ColumnDefault("false")
    private Boolean deleted = false;
}
