package com.dati.datasource.server.pojo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddTableRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String schema;

}
