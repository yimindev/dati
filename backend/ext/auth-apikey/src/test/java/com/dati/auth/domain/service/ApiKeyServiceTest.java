package com.dati.auth.domain.service;

import com.dati.auth.domain.model.ApiKey;
import com.dati.auth.repository.dao.ApiKeyRepository;
import com.dati.auth.repository.po.ApiKeyPO;
import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApiKeyService unit tests")
class ApiKeyServiceTest {

    @Mock
    private ApiKeyRepository repository;

    private ApiKeyService service;

    @BeforeEach
    void setUp() {
        service = new ApiKeyService(repository);
    }

    @Test
    void createGeneratesSkPrefixedKeyWithMaskAndHash() {
        when(repository.save(any(ApiKeyPO.class))).thenAnswer(inv -> inv.getArgument(0));

        ApiKeyService.CreateApiKeyResult result = service.create("user-1", "Claude Desktop", null);

        assertThat(result.plaintext()).startsWith("sk_");
        assertThat(result.plaintext().length()).isGreaterThan(40);
        assertThat(result.key().userId()).isEqualTo("user-1");
        assertThat(result.key().name()).isEqualTo("Claude Desktop");
        assertThat(result.key().expiresAt()).isNull();
        // hash is SHA-256 hex of plaintext, plaintext never stored
        assertThat(result.key().keyHash()).isNotEqualTo(result.plaintext());
        assertThat(result.key().keyHash()).hasSize(64);
        // mask keeps first 4 + last 4 chars of the random part
        String randomPart = result.plaintext().substring(3);
        assertThat(result.key().keyMask()).isEqualTo(
                "sk_" + randomPart.substring(0, 4) + "***" + randomPart.substring(randomPart.length() - 4));
        verify(repository).save(any(ApiKeyPO.class));
    }

    @Test
    void createWithExpirySetsExpiresAt() {
        when(repository.save(any(ApiKeyPO.class))).thenAnswer(inv -> inv.getArgument(0));

        ApiKeyService.CreateApiKeyResult result = service.create("user-1", "CI script", 30);

        assertThat(result.key().expiresAt()).isAfter(Instant.now());
        assertThat(result.key().expiresAt()).isBefore(Instant.now().plus(31, ChronoUnit.DAYS));
    }

    @Test
    void createWithBoundaryExpiryDaysAccepted() {
        when(repository.save(any(ApiKeyPO.class))).thenAnswer(inv -> inv.getArgument(0));

        ApiKeyService.CreateApiKeyResult shortKey = service.create("user-1", "short", 7);
        ApiKeyService.CreateApiKeyResult longKey = service.create("user-1", "long", 365);

        assertThat(shortKey.key().expiresAt()).isBefore(Instant.now().plus(8, ChronoUnit.DAYS));
        assertThat(longKey.key().expiresAt()).isBefore(Instant.now().plus(366, ChronoUnit.DAYS));
    }

    @Test
    void createWithInvalidExpiryRejects() {
        assertThatThrownBy(() -> service.create("user-1", "bad", 5))
                .isInstanceOf(DatiException.class)
                .extracting(e -> ((DatiException) e).getCode())
                .isEqualTo(ErrorCode.AUTH_KEY_INVALID_EXPIRY);
        verify(repository, never()).save(any());
    }

    @Test
    void listReturnsOnlyOwnKeys() {
        ApiKeyPO po = new ApiKeyPO();
        po.setId("k-1");
        po.setUserId("user-1");
        po.setKeyHash("h".repeat(64));
        po.setKeyMask("sk_ab12***cd34");
        when(repository.findByUserIdOrderByCreatedAtDesc("user-1")).thenReturn(List.of(po));

        List<ApiKey> keys = service.list("user-1");

        assertThat(keys).hasSize(1);
        assertThat(keys.getFirst().id()).isEqualTo("k-1");
    }

    @Test
    void deleteOwnKeyRemovesIt() {
        ApiKeyPO po = new ApiKeyPO();
        po.setId("k-1");
        po.setUserId("user-1");
        when(repository.findById("k-1")).thenReturn(Optional.of(po));

        service.delete("k-1", "user-1");

        verify(repository).delete(po);
    }

    @Test
    void deleteUnknownKeyIsIdempotent() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        service.delete("missing", "user-1");

        verify(repository, never()).delete(any());
    }

    @Test
    void deleteOtherUsersKeyRejects() {
        ApiKeyPO po = new ApiKeyPO();
        po.setId("k-1");
        po.setUserId("user-2");
        when(repository.findById("k-1")).thenReturn(Optional.of(po));

        assertThatThrownBy(() -> service.delete("k-1", "user-1"))
                .isInstanceOf(DatiException.class)
                .extracting(e -> ((DatiException) e).getCode())
                .isEqualTo(ErrorCode.AUTH_KEY_FORBIDDEN);
        verify(repository, never()).delete(any());
    }

    @Test
    void findByKeyHashReturnsModel() {
        ApiKeyPO po = new ApiKeyPO();
        po.setId("k-1");
        po.setUserId("user-1");
        po.setKeyHash("h".repeat(64));
        po.setKeyMask("sk_ab12***cd34");
        when(repository.findByKeyHash("h".repeat(64))).thenReturn(Optional.of(po));

        Optional<ApiKey> found = service.findByKeyHash("h".repeat(64));

        assertThat(found).isPresent();
        assertThat(found.get().userId()).isEqualTo("user-1");
    }

    @Test
    void markUsedUpdatesLastUsedAt() {
        ApiKeyPO po = new ApiKeyPO();
        po.setId("k-1");
        po.setUserId("user-1");
        when(repository.findById("k-1")).thenReturn(Optional.of(po));

        service.markUsed("k-1");

        assertThat(po.getLastUsedAt()).isNotNull();
        verify(repository).save(po);
    }
}
