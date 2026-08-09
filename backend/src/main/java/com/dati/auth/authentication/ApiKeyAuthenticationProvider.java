package com.dati.auth.authentication;

import com.dati.auth.domain.model.ApiKey;
import com.dati.auth.domain.service.ApiKeyService;
import com.dati.auth.repository.dao.UserRepository;
import com.dati.auth.repository.mapper.UserMapper;
import com.dati.auth.repository.po.UserPO;
import com.dati.auth.server.pojo.LoginRequest;
import com.dati.common.HashUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

/**
 * Authenticates requests carrying a user API key (Authorization: Bearer sk_...).
 * The sk_ prefix distinguishes API key requests from JWT requests. Because the
 * JWT provider (LocalAuthenticationProvider) claims any Bearer header, this
 * provider is ordered first (@Order(Ordered.HIGHEST_PRECEDENCE)) so sk_ requests
 * are routed here before the JWT provider can claim them.
 * API keys have no login flow (login() is unsupported by design).
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiKeyAuthenticationProvider implements AuthenticationProvider {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTH_HEADER = "Authorization";
    private static final String KEY_PREFIX = "sk_";

    private final ApiKeyService apiKeyService;
    private final UserRepository userRepository;

    public ApiKeyAuthenticationProvider(ApiKeyService apiKeyService,
                                        UserRepository userRepository) {
        this.apiKeyService = apiKeyService;
        this.userRepository = userRepository;
    }

    @Override
    public boolean canAuthenticate(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTH_HEADER);
        return authHeader != null
            && authHeader.startsWith(BEARER_PREFIX + KEY_PREFIX);
    }

    @Override
    public Optional<User> authenticate(HttpServletRequest request) {
        String token = request.getHeader(AUTH_HEADER).substring(BEARER_PREFIX.length());
        Optional<ApiKey> key = apiKeyService.findByKeyHash(HashUtils.sha256Hex(token));
        if (key.isEmpty() || key.get().isExpired(Instant.now())) {
            return Optional.empty();
        }
        Optional<UserPO> userPO = userRepository.findById(key.get().userId());
        if (userPO.isEmpty()) {
            return Optional.empty();
        }
        apiKeyService.markUsed(key.get().id());
        return Optional.of(UserMapper.toUser(userPO.get()));
    }

    @Override
    public String login(LoginRequest request) {
        throw new UnsupportedOperationException("API keys are not issued via login");
    }

    @Override
    public boolean supports(String type) {
        return false;
    }

}
