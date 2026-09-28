package com.healthbox.hms_backend.security.jwt;

import com.healthbox.hms_backend.modules.auth.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long expiration;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expirationMs
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.expiration = expirationMs;
    }

    /** Embeds the ABAC attributes (role, hospitalId, phno) as claims so every request carries tenant + role context. */
    public String generateToken(User user) {
        return generateToken(user, user.getHospitalId());
    }

    public String generateToken(User user, Long hospitalId) {
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("phno", user.getPhno())
                .claim("role", user.getRole().name())
                .claim("hospitalId", hospitalId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key)
                .compact();
    }

    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
