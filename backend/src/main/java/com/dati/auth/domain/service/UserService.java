package com.dati.auth.domain.service;

import com.dati.auth.authentication.LocalAuthenticationProvider;
import com.dati.auth.authentication.User;
import com.dati.auth.repository.dao.UserRepository;
import com.dati.auth.repository.mapper.UserMapper;
import com.dati.auth.repository.po.UserPO;
import com.dati.auth.server.pojo.RegisterRequest;
import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final LocalAuthenticationProvider localProvider;

    public UserService(UserRepository userRepository, LocalAuthenticationProvider localProvider) {
        this.userRepository = userRepository;
        this.localProvider = localProvider;
    }

    public String register(RegisterRequest request) {
        if (userRepository.existsByName(request.name())) {
            throw new DatiException(ErrorCode.AUTH_USER_EXISTS, request.name());
        }

        User user = new User();
        user.setName(request.name());
        user.setDisplayName(request.displayName());

        String passwordHash = localProvider.encodePassword(request.password());
        UserPO userPO = UserMapper.toUserPO(user, passwordHash);

        UserPO saved = userRepository.save(userPO);
        return saved.getId();
    }

    public Map<String, User> getUserMap(Collection<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(userIds).stream()
                .map(UserMapper::toUser)
                .collect(Collectors.toMap(User::getId, u -> u));
    }

}
