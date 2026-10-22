package com.dati.auth;

import com.dati.semantic.repository.dao.SemanticSearchDAO;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end checks for authentication semantics that depend on the whole web stack:
 * the interceptor must produce the unified {@code ErrorResponse} body, and usernames
 * must be unique / loginable regardless of casing.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Authentication error response tests")
class AuthenticationErrorResponseTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SemanticSearchDAO semanticSearchDAO;

    @Test
    @DisplayName("Invalid bearer token should return 401 with AUTH002 JSON body and Bearer challenge")
    void invalidToken_shouldReturnAuth002Body() throws Exception {
        mockMvc.perform(get("/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.code").value("AUTH002"))
                .andExpect(jsonPath("$.message").value("Token invalid or expired"));
    }

    @Test
    @DisplayName("Missing credentials should return 401 with AUTH009 JSON body")
    void missingToken_shouldReturnAuth009Body() throws Exception {
        mockMvc.perform(get("/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.code").value("AUTH009"))
                .andExpect(jsonPath("$.message").value("Authentication token is required"));
    }

    @Test
    @DisplayName("Usernames should be unique and loginable regardless of casing")
    void registerAndLogin_caseInsensitiveUsername() throws Exception {
        String name = "qa-ci-" + UUID.randomUUID().toString().substring(0, 8);
        String password = "QaTest123456";

        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(name, password)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(name.toUpperCase(), password)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AUTH003"));

        MvcResult login = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "local", "name": "%s", "password": "%s"}
                                """.formatted("  " + name.toUpperCase() + " ", password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        String token = JsonPath.read(login.getResponse().getContentAsString(), "$.token");

        mockMvc.perform(get("/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name));
    }

    @Test
    @DisplayName("Registration should reject passwords shorter than 8 characters")
    void register_shortPassword_shouldReturn400() throws Exception {
        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody("qa-short-" + UUID.randomUUID().toString().substring(0, 8), "x")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CM001"));
    }

    @Test
    @DisplayName("MCP endpoint should answer unauthenticated event-stream clients with the 401 JSON body")
    void mcpEndpoint_withoutToken_shouldReturn401Json() throws Exception {
        mockMvc.perform(post("/demo/mcp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.code").value("AUTH009"));
    }

    private static String registerBody(String name, String password) {
        return """
                {"name": "%s", "password": "%s", "display_name": "QA"}
                """.formatted(name, password);
    }

}
