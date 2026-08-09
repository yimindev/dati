package com.dati.auth.server.pojo;

import java.time.Instant;

public record ApiKeyVO(
        String id,
        String name,
        String keyMask,
        Instant createdAt,
        Instant expiresAt,
        Instant lastUsedAt) {
}
