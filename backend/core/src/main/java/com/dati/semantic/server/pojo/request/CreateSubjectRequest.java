package com.dati.semantic.server.pojo.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class CreateSubjectRequest {
    @NotBlank
    private String name;
    private String description;
    @NotBlank
    private String datasourceId;
    private List<String> aliases;
}
