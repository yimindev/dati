package com.dati.auth.domain.service;

import com.dati.auth.authentication.User;
import com.dati.auth.domain.model.LoginName;
import com.dati.auth.repository.dao.UserRepository;
import com.dati.auth.repository.mapper.UserMapper;
import com.dati.auth.repository.po.UserPO;
import com.dati.auth.server.pojo.RegisterRequest;
import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
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
        String name = LoginName.normalize(request.name());
        if (userRepository.existsByName(name)) {
            throw new DatiException(ErrorCode.AUTH_USER_EXISTS, name);
        }

        User user = new User();
        user.setName(name);
        user.setDisplayName(request.displayName());

        String passwordHash = passwordEncoder.encode(request.password());
        UserPO userPO = UserMapper.toUserPO(user, passwordHash);
        return save(userPO);
    }

    /** The unique index is the source of truth; a concurrent registration maps to AUTH003 instead of a 500. */
    private String save(UserPO userPO) {
        try {
            return userRepository.save(userPO).getId();
        } catch (DataIntegrityViolationException e) {
            throw new DatiException(ErrorCode.AUTH_USER_EXISTS, userPO.getName());
        }
    }

}
