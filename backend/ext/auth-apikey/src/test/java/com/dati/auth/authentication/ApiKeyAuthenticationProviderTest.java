package com.dati.auth.authentication;

import com.dati.auth.domain.model.ApiKey;
import com.dati.auth.domain.service.ApiKeyService;
import com.dati.auth.repository.dao.UserRepository;
import com.dati.auth.repository.po.UserPO;
import com.dati.common.HashUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApiKeyAuthenticationProvider unit tests")
class ApiKeyAuthenticationProviderTest {

    @Mock
    private ApiKeyService apiKeyService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest request;

    private ApiKeyAuthenticationProvider provider;

    @BeforeEach
    void setUp() {
        provider = new ApiKeyAuthenticationProvider(apiKeyService, userRepository);
    }

    @Test
    void canAuthenticateOnlySkPrefix() {
        when(request.getHeader("Authorization")).thenReturn("Bearer sk_abc123");
        assertThat(provider.canAuthenticate(request)).isTrue();

        when(request.getHeader("Authorization")).thenReturn("Bearer eyJhbGciOiJIUzI1NiJ9.abc");
        assertThat(provider.canAuthenticate(request)).isFalse();

        when(request.getHeader("Authorization")).thenReturn(null);
        assertThat(provider.canAuthenticate(request)).isFalse();
    }

    @Test
    void authenticateValidKeyReturnsBoundUserAndMarksUsed() {
        String plaintext = "sk_" + "a".repeat(40);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + plaintext);
        ApiKey key = new ApiKey("k-1", "user-1", "Claude Desktop",
                "h".repeat(64), "sk_aaaa***aaaa", null, null, Instant.now());
        when(apiKeyService.findByKeyHash(eq(HashUtils.sha256Hex(plaintext)))).thenReturn(Optional.of(key));
        UserPO userPO = new UserPO();
        userPO.setId("user-1");
        userPO.setName("alice");
        userPO.setDisplayName("Alice");
        when(userRepository.findById("user-1")).thenReturn(Optional.of(userPO));

        Optional<User> user = provider.authenticate(request);

        assertThat(user).isPresent();
        assertThat(user.get().getId()).isEqualTo("user-1");
        assertThat(user.get().getName()).isEqualTo("alice");
        verify(apiKeyService).markUsed("k-1");
    }

    @Test
    void authenticateExpiredKeyRejects() {
        String plaintext = "sk_" + "a".repeat(40);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + plaintext);
        ApiKey key = new ApiKey("k-1", "user-1", "old",
                "h".repeat(64), "sk_aaaa***aaaa",
                Instant.now().minus(1, ChronoUnit.DAYS), null, Instant.now());
        when(apiKeyService.findByKeyHash(eq(HashUtils.sha256Hex(plaintext)))).thenReturn(Optional.of(key));

        Optional<User> user = provider.authenticate(request);

        assertThat(user).isEmpty();
        verify(apiKeyService, never()).markUsed(anyString());
    }

    @Test
    void authenticateUnknownKeyRejects() {
        String plaintext = "sk_" + "b".repeat(40);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + plaintext);
        when(apiKeyService.findByKeyHash(anyString())).thenReturn(Optional.empty());

        Optional<User> user = provider.authenticate(request);

        assertThat(user).isEmpty();
        verify(userRepository, never()).findById(anyString());
    }
}
