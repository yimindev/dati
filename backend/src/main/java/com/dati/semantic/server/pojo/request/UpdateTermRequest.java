package com.dati.semantic.server.pojo.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class UpdateTermRequest {
    @NotBlank
    private String name;
    private String description;
    private List<String> aliases;
}
