package com.dati.auth.repository.mapper;

import com.dati.auth.domain.model.ApiKey;
import com.dati.auth.repository.po.ApiKeyPO;

public class ApiKeyMapper {

    public static ApiKeyPO toPO(ApiKey key) {
        if (key == null) {
            return null;
        }
        ApiKeyPO po = new ApiKeyPO();
        po.setId(key.id());
        po.setUserId(key.userId());
        po.setName(key.name());
        po.setKeyHash(key.keyHash());
        po.setKeyMask(key.keyMask());
        po.setExpiresAt(key.expiresAt());
        po.setLastUsedAt(key.lastUsedAt());
        return po;
    }

    public static ApiKey toModel(ApiKeyPO po) {
        if (po == null) {
            return null;
        }
        return new ApiKey(
                po.getId(), po.getUserId(), po.getName(), po.getKeyHash(),
                po.getKeyMask(), po.getExpiresAt(), po.getLastUsedAt(), po.getCreatedAt());
    }
}
