package com.dati.mcp.domain.model;

import lombok.Data;

@Data
public class PromptParameter {
    private String name;
    private String description;
    private boolean required;
}
