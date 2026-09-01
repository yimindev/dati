package com.dati.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Local credential mechanisms configuration.
 * Exposes the password hasher as a single bean so strength/algorithm tuning
 * happens in one place (see extension-guide.md for the module pattern).
 */
@Configuration
public class LocalAuthConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
