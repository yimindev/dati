package com.dati.auth.authentication;

import com.dati.auth.domain.service.JwtTokenHelper;
import com.dati.auth.repository.dao.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("LocalAuthenticationProvider Tests")
class LocalAuthenticationProviderTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final JwtTokenHelper jwtTokenHelper = mock(JwtTokenHelper.class);
    private final LocalAuthenticationProvider provider =
            new LocalAuthenticationProvider(userRepository, jwtTokenHelper, new BCryptPasswordEncoder());

    @Test
    @DisplayName("canAuthenticate: should return false when Authorization header is missing")
    void canAuthenticate_missingHeader_shouldReturnFalse() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn(null);

        assertThat(provider.canAuthenticate(request)).isFalse();
    }

    @Test
    @DisplayName("canAuthenticate: should return false when Authorization header is not Bearer")
    void canAuthenticate_nonBearerHeader_shouldReturnFalse() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Basic abc123");

        assertThat(provider.canAuthenticate(request)).isFalse();
    }

    @Test
    @DisplayName("canAuthenticate: should return true when Authorization header is Bearer")
    void canAuthenticate_bearerHeader_shouldReturnTrue() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");

        assertThat(provider.canAuthenticate(request)).isTrue();
    }

    @Test
    @DisplayName("authenticate: should return user when token is valid")
    void authenticate_validToken_shouldReturnUser() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");

        User expected = new User();
        expected.setId("1");
        expected.setName("alice");
        when(jwtTokenHelper.validateToken("valid-token")).thenReturn(expected);

        assertThat(provider.authenticate(request)).hasValue(expected);
    }

    @Test
    @DisplayName("authenticate: should return empty when token is invalid")
    void authenticate_invalidToken_shouldReturnEmpty() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");
        when(jwtTokenHelper.validateToken("invalid-token")).thenReturn(null);

        assertThat(provider.authenticate(request)).isEmpty();
    }
}
