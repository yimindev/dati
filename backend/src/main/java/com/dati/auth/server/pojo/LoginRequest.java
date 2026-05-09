package com.dati.auth.server.pojo;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String type,
        @NotBlank String name,
        @NotBlank String password) {
}
