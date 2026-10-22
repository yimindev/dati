package com.dati.auth.authentication;

import com.dati.auth.domain.service.AuthenticationService;
import com.dati.base.RequestContext;
import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import com.dati.config.Interceptor.AuthInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("AuthInterceptor Tests")
class AuthInterceptorTest {

    private HttpServletRequest request;
    private HttpServletResponse response;
    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        authenticationService = mock(AuthenticationService.class);
    }

    @AfterEach
    void tearDown() {
        RequestContext.getContext().clear();
    }

    @Test
    @DisplayName("Should succeed and set user when service returns user")
    void serviceReturnsUser_shouldSetRequestContextAndPass() {
        User user = new User();
        user.setId("1");
        user.setName("alice");
        when(authenticationService.authenticate(request)).thenReturn(Optional.of(user));

        AuthInterceptor interceptor = new AuthInterceptor(authenticationService);
        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        verify(response, never()).setStatus(anyInt());
        assertThat(RequestContext.getUser()).isEqualTo(user);
    }

    @Test
    @DisplayName("Should throw AUTH_TOKEN_INVALID with a Bearer challenge when the token is rejected")
    void serviceReturnsEmpty_withToken_shouldThrowTokenInvalid() {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer not.a.jwt");
        when(authenticationService.authenticate(request)).thenReturn(Optional.empty());

        AuthInterceptor interceptor = new AuthInterceptor(authenticationService);

        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                .isInstanceOf(DatiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_TOKEN_INVALID);
        verify(response).setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        assertThat(RequestContext.getUser()).isNull();
    }

    @Test
    @DisplayName("Should throw AUTH_TOKEN_REQUIRED when no credentials are supplied")
    void serviceReturnsEmpty_withoutToken_shouldThrowTokenRequired() {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);
        when(authenticationService.authenticate(request)).thenReturn(Optional.empty());

        AuthInterceptor interceptor = new AuthInterceptor(authenticationService);

        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                .isInstanceOf(DatiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_TOKEN_REQUIRED);
        verify(response).setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
    }

    @Test
    @DisplayName("Should bypass all checks when authenticationService is null (e.g. @WebMvcTest)")
    void serviceIsNull_shouldBypass() {
        AuthInterceptor interceptor = new AuthInterceptor(null);
        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        verify(authenticationService, never()).authenticate(request);
        verify(response, never()).setStatus(anyInt());
    }
}
