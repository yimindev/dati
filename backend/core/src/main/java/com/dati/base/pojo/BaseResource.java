package com.dati.base.pojo;

import lombok.Data;

import java.time.Instant;

/** Domain-model base for named resources. */
@Data
public class BaseResource {

    private String id;

    private String name;

    private String description;

    private String createdBy;

    private Instant createdAt;

    private String updatedBy;

    private Instant updatedAt;

}
