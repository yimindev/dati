package com.dati.auth.domain.service;

import com.dati.auth.repository.dao.UserRepository;
import com.dati.auth.repository.po.UserPO;
import com.dati.auth.server.pojo.RegisterRequest;
import com.dati.base.exception.DatiException;
import com.dati.base.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("RegistrationService Tests")
class RegistrationServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final RegistrationService registrationService =
            new RegistrationService(userRepository, new BCryptPasswordEncoder());

    @Test
    @DisplayName("Should successfully register new user with hashed password")
    void register_success() {
        RegisterRequest request = new RegisterRequest("alice", "Password123!", "Alice In Wonderland");
        when(userRepository.existsByName("alice")).thenReturn(false);

        UserPO savedPO = new UserPO();
        savedPO.setId("user-123");
        savedPO.setName("alice");
        savedPO.setDisplayName("Alice In Wonderland");
        when(userRepository.save(any(UserPO.class))).thenReturn(savedPO);

        String userId = registrationService.register(request);

        assertThat(userId).isEqualTo("user-123");

        ArgumentCaptor<UserPO> poCaptor = ArgumentCaptor.forClass(UserPO.class);
        verify(userRepository).save(poCaptor.capture());
        UserPO captured = poCaptor.getValue();
        assertThat(captured.getName()).isEqualTo("alice");
        assertThat(captured.getDisplayName()).isEqualTo("Alice In Wonderland");
        assertThat(captured.getPassword()).isNotEqualTo("Password123!");
    }

    @Test
    @DisplayName("Should throw AUTH_USER_EXISTS when username already exists")
    void register_duplicateUsername_shouldThrow() {
        RegisterRequest request = new RegisterRequest("alice", "Password123!", "Alice");
        when(userRepository.existsByName("alice")).thenReturn(true);

        assertThatThrownBy(() -> registrationService.register(request))
                .isInstanceOf(DatiException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_USER_EXISTS);
    }

}
