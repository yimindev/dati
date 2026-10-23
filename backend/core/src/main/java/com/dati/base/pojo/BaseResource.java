package com.dati.base.pojo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;

@Data
public class BaseResource {

    private String id;

    @NotBlank
    @Size(max = 64)
    private String name;

    @Size(max = 1024)
    private String description;

    private String createdBy;

    private Instant createdAt;

    private String updatedBy;

    private Instant updatedAt;

}
