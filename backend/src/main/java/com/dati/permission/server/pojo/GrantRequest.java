package com.dati.permission.server.pojo;

import com.dati.permission.domain.model.Permission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GrantRequest {

    @NotBlank
    private String principalType;

    @NotBlank
    private String principalId;

    @NotNull
    private Permission permission;
}
