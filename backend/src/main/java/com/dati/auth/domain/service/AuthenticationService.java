package com.dati.auth.domain.service;

import com.dati.auth.authentication.AuthenticationProvider;
import com.dati.auth.authentication.User;
import com.dati.auth.server.pojo.LoginRequest;
import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AuthenticationService {

    private final List<AuthenticationProvider> providers;

    public AuthenticationService(List<AuthenticationProvider> providers) {
        this.providers = providers;
    }

    public String login(LoginRequest request) {
        AuthenticationProvider provider = providers.stream()
                .filter(p -> p.supports(request.type()))
                .findFirst()
                .orElseThrow(() -> new DatiException(ErrorCode.AUTH_TYPE_UNSUPPORTED, request.type()));

        return provider.login(request);
    }

    public Optional<User> authenticate(HttpServletRequest request) {
        return providers.stream()
                .filter(p -> p.canAuthenticate(request))
                .findFirst().flatMap(provider -> provider.authenticate(request));
    }

}
