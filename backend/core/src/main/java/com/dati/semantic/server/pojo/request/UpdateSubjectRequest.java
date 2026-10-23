package com.dati.semantic.server.pojo.request;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

@Data
public class UpdateSubjectRequest {
    /** Null keeps the current name (partial update); blank is rejected. */
    @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank")
    private String name;
    private String description;
    private List<String> aliases;
}
