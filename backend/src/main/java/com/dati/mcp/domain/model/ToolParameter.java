package com.dati.mcp.domain.model;

import lombok.Data;

@Data
public class ToolParameter {
    private String name;
    private String type;    // String / Number / Boolean / Date / Array
    private boolean required;
    private String defaultValue;
    private String description;
}
