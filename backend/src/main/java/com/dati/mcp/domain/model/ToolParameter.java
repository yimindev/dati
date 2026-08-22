package com.dati.mcp.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolParameter {
    private String name;
    private String type;    // String / Number / Boolean / DateTime / Array
    private boolean required;
    private String defaultValue;
    private String description;
}
