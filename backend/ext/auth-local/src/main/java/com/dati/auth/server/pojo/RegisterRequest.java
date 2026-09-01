package com.dati.auth.server.pojo;

import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank String name,
        @NotBlank String password,
        String displayName) {
}
