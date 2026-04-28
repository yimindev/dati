package com.dati.semantic.server.pojo.request;

import com.dati.semantic.domain.SemanticEntityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LinkTermRelationRequest {
    @NotNull
    private SemanticEntityType entityType;
    @NotBlank
    private String tableId;
    private String fieldName;
}
