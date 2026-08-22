package com.dati.mcp.domain.service;

import com.dati.auth.authentication.User;
import com.dati.base.RequestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SystemVariableResolver unit tests")
class SystemVariableResolverTest {

    private SystemVariableResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new SystemVariableResolver();
        RequestContext.clear();
    }

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    @DisplayName("resolve - with authenticated user in RequestContext")
    void resolve_withUser() {
        User user = new User();
        user.setId("usr-123");
        user.setName("admin");
        user.setDisplayName("Administrator");
        RequestContext.setUser(user);

        Map<String, Object> vars = resolver.resolve();

        assertThat(vars)
            .containsEntry("_user.id", "usr-123")
            .containsEntry("_user.name", "admin")
            .containsEntry("_user.display_name", "Administrator")
            .containsKey("_now")
            .containsEntry("_date", LocalDate.now().toString());

        assertThat(vars.get("_now")).isNotNull();
    }

    @Test
    @DisplayName("resolve - without authenticated user (null context)")
    void resolve_withoutUser() {
        Map<String, Object> vars = resolver.resolve();

        assertThat(vars)
            .containsEntry("_user.id", null)
            .containsEntry("_user.name", null)
            .containsEntry("_user.display_name", null)
            .containsKey("_now")
            .containsEntry("_date", LocalDate.now().toString());

        assertThat(vars.get("_now")).isNotNull();
    }

    @Test
    @DisplayName("isSystemVariable - checks prefix and exact names")
    void isSystemVariable() {
        assertThat(SystemVariableResolver.isSystemVariable("_user.id")).isTrue();
        assertThat(SystemVariableResolver.isSystemVariable("_user.name")).isTrue();
        assertThat(SystemVariableResolver.isSystemVariable("_user.display_name")).isTrue();
        assertThat(SystemVariableResolver.isSystemVariable("_now")).isTrue();
        assertThat(SystemVariableResolver.isSystemVariable("_date")).isTrue();

        assertThat(SystemVariableResolver.isSystemVariable("user_id")).isFalse();
        assertThat(SystemVariableResolver.isSystemVariable("userId")).isFalse();
        assertThat(SystemVariableResolver.isSystemVariable("status")).isFalse();
        assertThat(SystemVariableResolver.isSystemVariable("_user.unknown")).isFalse();
        assertThat(SystemVariableResolver.isSystemVariable("_unknown")).isFalse();
    }
}
