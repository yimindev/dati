package com.dati.mcp.domain.service;

import com.dati.auth.authentication.User;
import com.dati.base.RequestContext;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Resolves built-in system parameters (_user.id, _user.name, _user.display_name, _now, _date)
 * from server-side security context (RequestContext) and system clock.
 */
@Component
public class SystemVariableResolver {

    private static final DateTimeFormatter DATETIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Keep in sync with frontend SYSTEM_VARIABLES in template-completions.ts
    public static final Set<String> SYSTEM_VARIABLES = Set.of(
        "_user.id",
        "_user.name",
        "_user.display_name",
        "_now",
        "_date"
    );

    /**
     * Resolves all system variables based on the current request context and system time.
     */
    public Map<String, Object> resolve() {
        Map<String, Object> vars = new HashMap<>();
        User user = RequestContext.getUser();
        if (user != null) {
            vars.put("_user.id", user.getId());
            vars.put("_user.name", user.getName());
            vars.put("_user.display_name", user.getDisplayName());
        } else {
            vars.put("_user.id", null);
            vars.put("_user.name", null);
            vars.put("_user.display_name", null);
        }

        LocalDateTime now = LocalDateTime.now();
        vars.put("_now", now.format(DATETIME_FORMATTER));
        vars.put("_date", now.toLocalDate().toString());
        return vars;
    }

    /**
     * Checks whether the given variable name represents a system-level built-in parameter.
     */
    public static boolean isSystemVariable(String name) {
        if (name == null) {
            return false;
        }
        return SYSTEM_VARIABLES.contains(name);
    }
}
