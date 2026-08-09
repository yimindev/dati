package com.dati.auth.domain.service;

import com.dati.auth.authentication.ApiKeyAuthenticationProvider;
import com.dati.auth.authentication.AuthenticationProvider;
import com.dati.auth.authentication.User;
import com.dati.auth.domain.model.ApiKey;
import com.dati.auth.repository.dao.UserRepository;
import com.dati.auth.repository.po.UserPO;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("AuthenticationService Tests")
class AuthenticationServiceTest {

    private final AuthenticationProvider localProvider = mock(AuthenticationProvider.class);
    private final AuthenticationProvider oauthProvider = mock(AuthenticationProvider.class);
    private final AuthenticationService service = new AuthenticationService(List.of(localProvider, oauthProvider));

    @Test
    @DisplayName("Should return empty when no provider claims the request")
    void noProviderClaims_shouldReturnEmpty() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(localProvider.canAuthenticate(request)).thenReturn(false);
        when(oauthProvider.canAuthenticate(request)).thenReturn(false);

        Optional<User> result = service.authenticate(request);

        assertThat(result).isEmpty();
        verify(localProvider).canAuthenticate(request);
        verify(oauthProvider).canAuthenticate(request);
        verify(localProvider, never()).authenticate(request);
        verify(oauthProvider, never()).authenticate(request);
    }

    @Test
    @DisplayName("Should return user when first provider claims and authenticates")
    void firstClaimsAndSucceeds_shouldReturnUser_andNotCallSecond() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        User user = new User();
        user.setId("1");
        user.setName("alice");
        when(localProvider.canAuthenticate(request)).thenReturn(true);
        when(localProvider.authenticate(request)).thenReturn(Optional.of(user));

        Optional<User> result = service.authenticate(request);

        assertThat(result).hasValue(user);
        verify(localProvider).canAuthenticate(request);
        verify(localProvider).authenticate(request);
        verify(oauthProvider, never()).canAuthenticate(request);
        verify(oauthProvider, never()).authenticate(request);
    }

    @Test
    @DisplayName("Should return empty when first provider claims but authentication fails")
    void firstClaimsButFails_shouldReturnEmpty_andNotCallSecond() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(localProvider.canAuthenticate(request)).thenReturn(true);
        when(localProvider.authenticate(request)).thenReturn(Optional.empty());

        Optional<User> result = service.authenticate(request);

        assertThat(result).isEmpty();
        verify(localProvider).canAuthenticate(request);
        verify(localProvider).authenticate(request);
        verify(oauthProvider, never()).canAuthenticate(request);
        verify(oauthProvider, never()).authenticate(request);
    }

    @Test
    @DisplayName("Should skip first provider and try next when first does not claim")
    void firstDoesNotClaim_secondClaimsAndSucceeds_shouldReturnUser() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        User user = new User();
        user.setId("2");
        user.setName("bob");
        when(localProvider.canAuthenticate(request)).thenReturn(false);
        when(oauthProvider.canAuthenticate(request)).thenReturn(true);
        when(oauthProvider.authenticate(request)).thenReturn(Optional.of(user));

        Optional<User> result = service.authenticate(request);

        assertThat(result).hasValue(user);
        verify(localProvider).canAuthenticate(request);
        verify(localProvider, never()).authenticate(request);
        verify(oauthProvider).canAuthenticate(request);
        verify(oauthProvider).authenticate(request);
    }

    @Test
    @DisplayName("Should route sk_ bearer requests to ApiKeyAuthenticationProvider")
    void skBearerRequest_shouldRouteToApiKeyProvider() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer sk_abc123");

        ApiKeyService apiKeyService = mock(ApiKeyService.class);
        UserRepository userRepository = mock(UserRepository.class);
        ApiKey apiKey = new ApiKey("k-1", "user-1", "Claude Desktop",
                "h".repeat(64), "sk_ab12***cd34", null, null, Instant.now());
        when(apiKeyService.findByKeyHash(anyString())).thenReturn(Optional.of(apiKey));
        UserPO userPO = new UserPO();
        userPO.setId("user-1");
        userPO.setName("alice");
        userPO.setDisplayName("Alice");
        when(userRepository.findById("user-1")).thenReturn(Optional.of(userPO));

        AuthenticationProvider apiKeyProvider =
            new ApiKeyAuthenticationProvider(apiKeyService, userRepository);
        AuthenticationService service =
            new AuthenticationService(List.of(apiKeyProvider, localProvider));

        Optional<User> result = service.authenticate(request);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("user-1");
        // JWT provider must not even be consulted for sk_ requests
        verify(localProvider, never()).canAuthenticate(request);
    }

    @Test
    @DisplayName("Should skip ApiKeyAuthenticationProvider for non-sk_ bearer requests")
    void jwtBearerRequest_shouldSkipApiKeyProvider() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer eyJhbGciOiJIUzI1NiJ9.abc");

        ApiKeyService apiKeyService = mock(ApiKeyService.class);
        ApiKeyAuthenticationProvider apiKeyProvider =
            new ApiKeyAuthenticationProvider(apiKeyService, mock(UserRepository.class));
        AuthenticationService service =
            new AuthenticationService(List.of(localProvider, apiKeyProvider));

        Optional<User> result = service.authenticate(request);

        assertThat(result).isEmpty();
        verify(apiKeyService, never()).findByKeyHash(anyString());
    }
}
