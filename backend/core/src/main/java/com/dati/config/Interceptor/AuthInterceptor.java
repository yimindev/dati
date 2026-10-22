package com.dati.config.Interceptor;

import com.dati.auth.authentication.User;
import com.dati.auth.domain.service.AuthenticationService;
import com.dati.base.RequestContext;
import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final String BEARER_CHALLENGE = "Bearer";

    private final AuthenticationService authenticationService;

    public AuthInterceptor(@Autowired(required = false) AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    /**
     * Rejects unauthenticated requests with the unified {@code ErrorResponse} body
     * (AUTH002 for a rejected token, AUTH009 when no credentials were supplied).
     */
    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        if (authenticationService == null) {
            return true;
        }
        Optional<User> user = authenticationService.authenticate(request);
        if (user.isPresent()) {
            RequestContext.setUser(user.get());
            return true;
        }
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, BEARER_CHALLENGE);
        throw new DatiException(hasCredentials(request)
                ? ErrorCode.AUTH_TOKEN_INVALID
                : ErrorCode.AUTH_TOKEN_REQUIRED);
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, Exception ex) {
        RequestContext.clear();
    }

    private static boolean hasCredentials(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        return authorization != null && !authorization.isBlank();
    }

}
