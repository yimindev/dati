package com.dati.semantic.server.pojo.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateTermRequest {
    @NotBlank
    @Size(max = 64)
    private String name;

    @Size(max = 1024)
    private String description;
    private List<String> aliases;
}
