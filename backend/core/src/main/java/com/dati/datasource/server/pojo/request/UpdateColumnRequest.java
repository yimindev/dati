package com.dati.datasource.server.pojo.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Partial update: every field is optional, omitted fields keep their stored value.
 */
@Data
public class UpdateColumnRequest {

    @Size(max = 64)
    @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank")
    private String name;

    @Size(max = 1024)
    private String description;

    @Size(max = 64)
    private String columnType;

    private List<String> aliases;

    private Boolean extractValueEnabled;

}
