package com.dati.semantic.server.pojo.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UpdateSubjectRequest {
    /** Null keeps the current name (partial update); blank is rejected. */
    @Pattern(regexp = "(?s).*\\S.*", message = "{validation.notBlank}")
    @Size(max = 64)
    private String name;

    @Size(max = 1024)
    private String description;
    private List<String> aliases;
}
