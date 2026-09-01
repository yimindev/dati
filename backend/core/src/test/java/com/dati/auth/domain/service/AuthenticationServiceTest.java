package com.dati.auth.domain.service;

import com.dati.auth.authentication.AuthenticationProvider;
import com.dati.auth.authentication.User;
import com.dati.auth.server.pojo.LoginRequest;
import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    @DisplayName("Should safely return empty when zero providers are configured")
    void zeroProviders_shouldReturnEmpty() {
        AuthenticationService emptyService = new AuthenticationService(List.of());
        HttpServletRequest request = mock(HttpServletRequest.class);

        Optional<User> result = emptyService.authenticate(request);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should route login to supporting provider")
    void login_shouldRouteToSupportingProvider() {
        LoginRequest request = new LoginRequest("local", "alice", "pass");
        when(localProvider.supports("local")).thenReturn(true);
        when(localProvider.login(request)).thenReturn("mock-jwt-token");

        String token = service.login(request);

        assertThat(token).isEqualTo("mock-jwt-token");
        verify(localProvider).login(request);
        verify(oauthProvider, never()).login(any());
    }

    @Test
    @DisplayName("Should throw AUTH_TYPE_UNSUPPORTED when no provider supports login type")
    void login_unsupportedType_shouldThrow() {
        LoginRequest request = new LoginRequest("ldap", "alice", "pass");
        when(localProvider.supports("ldap")).thenReturn(false);
        when(oauthProvider.supports("ldap")).thenReturn(false);

        assertThatThrownBy(() -> service.login(request))
                .isInstanceOf(DatiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_TYPE_UNSUPPORTED);
    }
}
