package com.dati.semantic.server.pojo.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddTableToSubjectRequest {
    @NotBlank
    private String tableId;
}
