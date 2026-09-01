package com.dati.auth.server.pojo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApiKeyCreateRequest(
        @NotBlank @Size(max = 64) String name,
        Integer expiresInDays) {
}
