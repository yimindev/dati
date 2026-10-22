package com.dati.auth.server.pojo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * {@code password} is bounded by 8 chars minimum (weak-credential floor) and 72 chars
 * maximum (BCrypt ignores everything past 72 bytes); {@code name} / {@code displayName}
 * match the column lengths so oversized input is a 400 instead of a 500.
 */
public record RegisterRequest(
        @NotBlank @Size(min = 1, max = 64, message = "must be between 1 and 64 characters") String name,
        @NotBlank @Size(min = 8, max = 72, message = "must be between 8 and 72 characters") String password,
        @Size(max = 64, message = "must be at most 64 characters") String displayName) {
}
