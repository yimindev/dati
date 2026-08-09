package com.dati.auth.domain.service;

import com.dati.auth.domain.model.ApiKey;
import com.dati.auth.repository.dao.ApiKeyRepository;
import com.dati.auth.repository.mapper.ApiKeyMapper;
import com.dati.auth.repository.po.ApiKeyPO;
import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import com.dati.common.HashUtils;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class ApiKeyService {

    private static final String KEY_PREFIX = "sk_";
    private static final int KEY_BYTES = 32;
    private static final Set<Integer> ALLOWED_EXPIRY_DAYS = Set.of(7, 30, 90, 180, 365);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyService(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    /** Created key with its plaintext; the plaintext is returned only once. */
    public record CreateApiKeyResult(ApiKey key, String plaintext) {
    }

    public CreateApiKeyResult create(String userId, String name, Integer expiresInDays) {
        if (expiresInDays != null && !ALLOWED_EXPIRY_DAYS.contains(expiresInDays)) {
            throw new DatiException(ErrorCode.AUTH_KEY_INVALID_EXPIRY);
        }
        String plaintext = generatePlaintext();
        ApiKey key = new ApiKey(
                null, userId, name, HashUtils.sha256Hex(plaintext), mask(plaintext),
                expiresInDays == null ? null : Instant.now().plusSeconds(expiresInDays * 86400L),
                null, null);
        ApiKeyPO saved = apiKeyRepository.save(ApiKeyMapper.toPO(key));
        return new CreateApiKeyResult(ApiKeyMapper.toModel(saved), plaintext);
    }

    public List<ApiKey> list(String userId) {
        return apiKeyRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(ApiKeyMapper::toModel)
                .toList();
    }

    /** Hard delete; unknown id is a no-op (idempotent), other users' keys are rejected. */
    public void delete(String id, String userId) {
        apiKeyRepository.findById(id).ifPresent(key -> {
            if (!key.getUserId().equals(userId)) {
                throw new DatiException(ErrorCode.AUTH_KEY_FORBIDDEN);
            }
            apiKeyRepository.delete(key);
        });
    }

    public Optional<ApiKey> findByKeyHash(String keyHash) {
        return apiKeyRepository.findByKeyHash(keyHash).map(ApiKeyMapper::toModel);
    }

    public void markUsed(String id) {
        apiKeyRepository.findById(id).ifPresent(key -> {
            key.setLastUsedAt(Instant.now());
            apiKeyRepository.save(key);
        });
    }

    private static String generatePlaintext() {
        byte[] bytes = new byte[KEY_BYTES];
        RANDOM.nextBytes(bytes);
        return KEY_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** sk_ + first 4 chars + *** + last 4 chars of the random part, e.g. sk_ab12***cd34. */
    private static String mask(String plaintext) {
        String randomPart = plaintext.substring(KEY_PREFIX.length());
        return KEY_PREFIX + randomPart.substring(0, 4) + "***" + randomPart.substring(randomPart.length() - 4);
    }
}
