package com.dati.auth.server.controller;

import com.dati.auth.authentication.User;
import com.dati.auth.domain.model.ApiKey;
import com.dati.auth.domain.service.ApiKeyService;
import com.dati.base.RequestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApiKeyController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ApiKeyController integration tests")
class ApiKeyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApiKeyService apiKeyService;

    @BeforeEach
    void setUpUser() {
        User user = new User();
        user.setId("user-1");
        user.setName("alice");
        RequestContext.setUser(user);
    }

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void createReturnsPlaintextOnce() throws Exception {
        ApiKey key = new ApiKey("k-1", "user-1", "Claude Desktop",
                "h".repeat(64), "sk_ab12***cd34", null, null, Instant.now());
        when(apiKeyService.create(eq("user-1"), eq("Claude Desktop"), eq(null)))
                .thenReturn(new ApiKeyService.CreateApiKeyResult(key, "sk_" + "x".repeat(40)));

        mockMvc.perform(post("/v1/auth/api-keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Claude Desktop\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value("sk_" + "x".repeat(40)))
                .andExpect(jsonPath("$.key_mask").value("sk_ab12***cd34"));
    }

    @Test
    void listReturnsMaskedKeys() throws Exception {
        ApiKey key = new ApiKey("k-1", "user-1", "CI script",
                "h".repeat(64), "sk_ab12***cd34", Instant.now().plusSeconds(86400), Instant.now(), Instant.now());
        when(apiKeyService.list("user-1")).thenReturn(List.of(key));

        mockMvc.perform(get("/v1/auth/api-keys"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("CI script"))
                .andExpect(jsonPath("$[0].key_mask").value("sk_ab12***cd34"))
                .andExpect(jsonPath("$[0].expires_at").exists());
    }

    @Test
    void deleteOwnKeyReturnsNoContent() throws Exception {
        doNothing().when(apiKeyService).delete(eq("k-1"), eq("user-1"));

        mockMvc.perform(delete("/v1/auth/api-keys/k-1"))
                .andExpect(status().isNoContent());

        verify(apiKeyService).delete("k-1", "user-1");
    }
}
