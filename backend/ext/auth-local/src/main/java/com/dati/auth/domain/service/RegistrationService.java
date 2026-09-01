package com.dati.auth.domain.service;

import com.dati.auth.authentication.User;
import com.dati.auth.repository.dao.UserRepository;
import com.dati.auth.repository.mapper.UserMapper;
import com.dati.auth.repository.po.UserPO;
import com.dati.auth.server.pojo.RegisterRequest;
import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RegistrationService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public RegistrationService(UserRepository userRepository,
                               BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public String register(RegisterRequest request) {
        if (userRepository.existsByName(request.name())) {
            throw new DatiException(ErrorCode.AUTH_USER_EXISTS, request.name());
        }

        User user = new User();
        user.setName(request.name());
        user.setDisplayName(request.displayName());

        String passwordHash = passwordEncoder.encode(request.password());
        UserPO userPO = UserMapper.toUserPO(user, passwordHash);

        UserPO saved = userRepository.save(userPO);
        return saved.getId();
    }

}
