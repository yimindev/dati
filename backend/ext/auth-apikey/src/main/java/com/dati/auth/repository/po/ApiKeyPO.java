package com.dati.auth.repository.po;

import com.dati.base.pojo.BasePO;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "api_key")
public class ApiKeyPO extends BasePO {

    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    @Column(length = 64, nullable = false)
    private String name;

    @Column(name = "key_hash", length = 64, unique = true, nullable = false)
    private String keyHash;

    @Column(name = "key_mask", length = 32, nullable = false)
    private String keyMask;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

}
