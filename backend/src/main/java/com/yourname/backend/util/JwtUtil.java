package com.yourname.backend.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;      // from application.properties

    private SecretKey secretKey;   // will hold the HMAC key object

    // 24h expiration in milliseconds
    private final long EXPIRATION_TIME = 24 * 60 * 60 * 1000;

    @PostConstruct
    public void init() {
        // build the signing key *once* from your fixed secret
        // Ensure the secret is sufficiently long and random for HS256
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generates a JWT token for the given username.
     * @param username The subject claim for the token.
     * @return The compacted JWT string.
     */
    public String generateToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + EXPIRATION_TIME);

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey, SignatureAlgorithm.HS256) // Use the SecretKey object
                .compact();
    }

    /**
     * Validates the token's signature and expiration, then extracts the username (subject).
     * @param token The JWT string to parse and validate.
     * @return The username (subject) from the token claims.
     * @throws io.jsonwebtoken.JwtException if the token is invalid (expired, malformed, wrong signature, etc.)
     */
    public String validateTokenAndGetUsername(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey) // Use the SecretKey object for verification
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }
}