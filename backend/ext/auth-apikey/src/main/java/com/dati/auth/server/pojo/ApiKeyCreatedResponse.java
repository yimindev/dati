package com.dati.auth.server.pojo;

import java.time.Instant;

/** Creation response: plaintext key returned exactly once. */
public record ApiKeyCreatedResponse(
        String id,
        String name,
        String key,
        String keyMask,
        Instant expiresAt) {
}
