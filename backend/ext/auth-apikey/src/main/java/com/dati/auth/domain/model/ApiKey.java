package com.dati.auth.domain.model;

import java.time.Instant;

/** User-scoped API key (opaque, sk_ prefixed). Plaintext is never stored. */
public record ApiKey(
        String id,
        String userId,
        String name,
        String keyHash,
        String keyMask,
        Instant expiresAt,
        Instant lastUsedAt,
        Instant createdAt) {

    public boolean isExpired(Instant now) {
        return expiresAt != null && now.isAfter(expiresAt);
    }
}
