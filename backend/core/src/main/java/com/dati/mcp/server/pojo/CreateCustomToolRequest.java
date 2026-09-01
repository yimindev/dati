package com.dati.mcp.server.pojo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Create-only request for custom tools. Extends {@link CustomToolRequest} (used for
 * updates) and adds the description requirement: PRD US-03 mandates description on
 * creation ("必填，帮助 LLM 理解用途"), while partial updates (e.g. the enabled toggle)
 * must be allowed without it.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CreateCustomToolRequest extends CustomToolRequest {

    @NotBlank(message = "description is required")
    private String description;
}
