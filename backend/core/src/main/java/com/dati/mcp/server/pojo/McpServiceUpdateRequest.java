package com.dati.mcp.server.pojo;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Partial update: every field is optional, omitted fields keep their stored value.
 */
@Data
public class McpServiceUpdateRequest {

    @Size(max = 64)
    @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank")
    private String name;

    @Size(max = 1024)
    private String description;

}
