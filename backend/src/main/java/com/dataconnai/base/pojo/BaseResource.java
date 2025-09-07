package com.dataconnai.base.pojo;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class BaseResource {

    private String id;

    private String name;

    private String description;

    private String createdBy;

    private OffsetDateTime createdAt;

    private String updatedBy;

    private OffsetDateTime updatedAt;

}
