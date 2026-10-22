package com.dati.auth.server.controller;

import com.dati.auth.domain.service.RegistrationService;
import com.dati.auth.server.pojo.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RegisterController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("RegisterController integration tests")
class RegisterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegistrationService registrationService;

    @Test
    @DisplayName("Should successfully register and return user id")
    void register_success() throws Exception {
        when(registrationService.register(any(RegisterRequest.class))).thenReturn("user-abc");

        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "alice",
                                  "password": "Password123!",
                                  "display_name": "Alice"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("user-abc"));

        verify(registrationService).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("Should reject single-character password")
    void register_tooShortPassword_shouldReturn400() throws Exception {
        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "alice",
                                  "password": "x"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CM001"))
                .andExpect(jsonPath("$.message").value(containsString("password")));

        verifyNoInteractions(registrationService);
    }

    @Test
    @DisplayName("Should accept password with exactly the minimum length")
    void register_minimumLengthPassword_shouldSucceed() throws Exception {
        when(registrationService.register(any(RegisterRequest.class))).thenReturn("user-abc");

        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "alice",
                                  "password": "12345678"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("user-abc"));
    }

    @Test
    @DisplayName("Should reject overly long name/display_name instead of failing with 500")
    void register_tooLongFields_shouldReturn400() throws Exception {
        String tooLong = "a".repeat(65);

        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "password": "Password123!"
                                }
                                """.formatted(tooLong)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CM001"))
                .andExpect(jsonPath("$.message").value(containsString("name")));

        verifyNoInteractions(registrationService);
    }

}
