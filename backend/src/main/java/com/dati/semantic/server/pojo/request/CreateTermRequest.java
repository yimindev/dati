package com.dati.semantic.server.pojo.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateTermRequest {
    @NotBlank
    private String name;
    private String description;
}
