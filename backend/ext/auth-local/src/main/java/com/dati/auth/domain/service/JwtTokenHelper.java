package com.dati.auth.domain.service;

import com.dati.auth.authentication.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenHelper {

    private final SecretKey secretKey;
    private final long expirationSeconds;

    public JwtTokenHelper(
            @Value("${auth.jwt.secret:change-me-in-production}") String secret,
            @Value("${auth.jwt.expiration-seconds:604800}") long expirationSeconds) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationSeconds = expirationSeconds;
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(expirationSeconds);

        return Jwts.builder()
                .subject(user.getId())
                .claim("name", user.getName())
                .claim("display_name", user.getDisplayName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(secretKey)
                .compact();
    }

    public User validateToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            User user = new User();
            user.setId(claims.getSubject());
            user.setName(claims.get("name", String.class));
            user.setDisplayName(claims.get("display_name", String.class));
            return user;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            return null;
        }
    }

}
