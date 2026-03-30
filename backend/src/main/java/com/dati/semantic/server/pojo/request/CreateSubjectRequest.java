package com.dati.semantic.server.pojo.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateSubjectRequest {
    @NotBlank
    private String name;
    private String description;
    @NotBlank
    private String datasourceId;
}
