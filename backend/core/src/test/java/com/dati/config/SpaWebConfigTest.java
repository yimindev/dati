package com.dati.config;

import com.dati.semantic.repository.dao.SemanticSearchDAO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("SpaWebConfig Tests")
class SpaWebConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SemanticSearchDAO semanticSearchDAO;

    // --- SPA ---

    @Test
    @DisplayName("Serve SPA root via Boot welcome page and index.html directly")
    void shouldServeSpaRootIndexHtml() throws Exception {
        // / is Boot's welcome-page mechanism (forward to index.html, not resolved directly)
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));

        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("DatI SPA Root")));
    }

    @Test
    @DisplayName("Fall back SPA route to index.html by direct resource resolution, not forward")
    void shouldFallbackSpaRouteToIndexHtml() throws Exception {
        MvcResult result = mockMvc.perform(get("/data-sources"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("DatI SPA Root")))
                .andReturn();
        assertNull(result.getResponse().getForwardedUrl());
    }

    @Test
    @DisplayName("Fall back arbitrarily deep SPA route to index.html")
    void shouldFallbackDeepSpaRouteToIndexHtml() throws Exception {
        mockMvc.perform(get("/a/b/c/d/e"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("DatI SPA Root")));
    }

    @Test
    @DisplayName("Serve static assets directly")
    void shouldServeStaticAssetDirectly() throws Exception {
        mockMvc.perform(get("/assets/app.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("DatI Static App JS")));
    }

    // --- Docs ---

    @Test
    @DisplayName("Redirect /docs to /docs/ for VitePress relative links")
    void shouldRedirectDocsToTrailingSlash() throws Exception {
        mockMvc.perform(get("/docs"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/docs/"));
    }

    @Test
    @DisplayName("Serve docs index at /docs/ and /docs/index.html")
    void shouldServeDocsIndex() throws Exception {
        // /docs/ has an empty path-within-pattern which the resource handler
        // short-circuits, so it is forwarded to /docs/index.html (served directly in a real container).
        mockMvc.perform(get("/docs/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/docs/index.html"));

        mockMvc.perform(get("/docs/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("DatI Documentation Root")));
    }

    @Test
    @DisplayName("Serve locale directory index at /docs/en/")
    void shouldServeLocaleDirectoryIndex() throws Exception {
        mockMvc.perform(get("/docs/en/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("DatI Documentation EN Root")));
    }

    @Test
    @DisplayName("Serve docs clean URL /docs/faq by direct resource resolution, not forward")
    void shouldServeDocsCleanUrl() throws Exception {
        MvcResult result = mockMvc.perform(get("/docs/faq"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("DatI FAQ Page")))
                .andReturn();
        assertNull(result.getResponse().getForwardedUrl());

        mockMvc.perform(get("/docs/faq.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("DatI FAQ Page")));
    }

    @Test
    @DisplayName("Return 404 for unknown docs page instead of SPA fallback")
    void shouldReturn404ForUnknownDocsPage() throws Exception {
        mockMvc.perform(get("/docs/nonexistent"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(containsString("DatI SPA Root"))));
    }

    // --- API and exclusions ---

    @Test
    @DisplayName("Keep authenticated API routes intact")
    void shouldKeepApiRoutesIntact() throws Exception {
        mockMvc.perform(get("/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(not(containsString("DatI SPA Root"))));
    }

    @Test
    @DisplayName("Return 404 for unknown public API path instead of 500 or SPA fallback")
    void shouldReturn404ForUnknownPublicApiPath() throws Exception {
        mockMvc.perform(get("/v1/public/nonexistent"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(containsString("DatI SPA Root"))));
    }

    @Test
    @DisplayName("Return 404 for actuator paths instead of SPA fallback")
    void shouldReturn404ForActuatorPath() throws Exception {
        mockMvc.perform(get("/actuator"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(containsString("DatI SPA Root"))));
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(containsString("DatI SPA Root"))));
    }

    @Test
    @DisplayName("Return 404 for missing file-like path instead of SPA fallback")
    void shouldReturn404ForFileLikePath() throws Exception {
        mockMvc.perform(get("/favicon.ico"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Keep MCP protocol routes intact")
    void shouldKeepMcpRoutesIntact() throws Exception {
        mockMvc.perform(post("/test-service/mcp"))
                .andExpect(status().isUnauthorized());
    }
}
