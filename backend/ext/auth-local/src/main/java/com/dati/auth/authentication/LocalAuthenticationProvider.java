package com.dati.auth.authentication;

import com.dati.auth.domain.service.JwtTokenHelper;
import com.dati.auth.repository.dao.UserRepository;
import com.dati.auth.repository.mapper.UserMapper;
import com.dati.auth.repository.po.UserPO;
import com.dati.auth.server.pojo.LoginRequest;
import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class LocalAuthenticationProvider implements AuthenticationProvider {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTH_HEADER = "Authorization";
    private static final String TYPE = "local";

    private final UserRepository userRepository;
    private final JwtTokenHelper jwtTokenHelper;
    private final BCryptPasswordEncoder passwordEncoder;

    public LocalAuthenticationProvider(UserRepository userRepository,
                                       JwtTokenHelper jwtTokenHelper,
                                       BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtTokenHelper = jwtTokenHelper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public boolean canAuthenticate(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTH_HEADER);
        return authHeader != null && authHeader.startsWith(BEARER_PREFIX);
    }

    @Override
    public Optional<User> authenticate(HttpServletRequest request) {
        String token = request.getHeader(AUTH_HEADER).substring(BEARER_PREFIX.length());
        User user = jwtTokenHelper.validateToken(token);
        return Optional.ofNullable(user);
    }

    @Override
    public String login(LoginRequest request) {
        UserPO userPO = userRepository.findByName(request.name())
                .orElseThrow(() -> new DatiException(ErrorCode.AUTH_LOGIN_FAILED));

        if (!passwordEncoder.matches(request.password(), userPO.getPassword())) {
            throw new DatiException(ErrorCode.AUTH_LOGIN_FAILED);
        }

        User user = UserMapper.toUser(userPO);
        return jwtTokenHelper.generateToken(user);
    }

    @Override
    public boolean supports(String type) {
        return TYPE.equals(type);
    }

}
