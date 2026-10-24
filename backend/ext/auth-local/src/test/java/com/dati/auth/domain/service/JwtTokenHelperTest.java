package com.dati.auth.domain.service;

import com.dati.auth.authentication.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtTokenHelper tests")
class JwtTokenHelperTest {

    private static final String SECRET = "0123456789abcdefghijklmnopqrstuvwxyz";
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    private final JwtTokenHelper helper = new JwtTokenHelper(SECRET, 3600);

    @Test
    @DisplayName("round-trips a generated token")
    void validateToken_shouldAcceptGeneratedToken() {
        User user = new User();
        user.setId("user-1");
        user.setName("qa-e2e-tester");
        user.setDisplayName("QA");

        User validated = helper.validateToken(helper.generateToken(user));

        assertThat(validated).isNotNull();
        assertThat(validated.getId()).isEqualTo("user-1");
        assertThat(validated.getName()).isEqualTo("qa-e2e-tester");
        assertThat(validated.getDisplayName()).isEqualTo("QA");
    }

    @Test
    @DisplayName("rejects a token without an expiration")
    void validateToken_shouldRejectMissingExpiration() {
        String token = Jwts.builder()
                .subject("user-1")
                .claim("name", "qa-e2e-tester")
                .issuedAt(Date.from(Instant.now()))
                .signWith(KEY)
                .compact();

        assertThat(helper.validateToken(token)).isNull();
    }

    @Test
    @DisplayName("rejects a token without a subject")
    void validateToken_shouldRejectMissingSubject() {
        String token = Jwts.builder()
                .claim("name", "qa-e2e-tester")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(KEY)
                .compact();

        assertThat(helper.validateToken(token)).isNull();
    }

    @Test
    @DisplayName("rejects a token issued in the future")
    void validateToken_shouldRejectFutureIssuedAt() {
        Instant future = Instant.now().plusSeconds(86_400);
        String token = Jwts.builder()
                .subject("user-1")
                .claim("name", "qa-e2e-tester")
                .issuedAt(Date.from(future))
                .expiration(Date.from(future.plusSeconds(3600)))
                .signWith(KEY)
                .compact();

        assertThat(helper.validateToken(token)).isNull();
    }

    @Test
    @DisplayName("rejects an expired token")
    void validateToken_shouldRejectExpiredToken() {
        Instant past = Instant.now().minusSeconds(7200);
        String token = Jwts.builder()
                .subject("user-1")
                .claim("name", "qa-e2e-tester")
                .issuedAt(Date.from(past))
                .expiration(Date.from(past.plusSeconds(60)))
                .signWith(KEY)
                .compact();

        assertThat(helper.validateToken(token)).isNull();
    }
}
