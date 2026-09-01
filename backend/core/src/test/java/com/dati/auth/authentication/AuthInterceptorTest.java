package com.dati.auth.authentication;

import com.dati.auth.domain.service.AuthenticationService;
import com.dati.base.RequestContext;
import com.dati.config.Interceptor.AuthInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
    @DisplayName("Should return 401 when service returns empty")
    void serviceReturnsEmpty_shouldReturn401() {
        when(authenticationService.authenticate(request)).thenReturn(Optional.empty());

        AuthInterceptor interceptor = new AuthInterceptor(authenticationService);
        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isFalse();
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertThat(RequestContext.getUser()).isNull();
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
