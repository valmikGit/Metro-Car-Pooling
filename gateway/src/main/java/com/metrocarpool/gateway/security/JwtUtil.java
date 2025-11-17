package com.metrocarpool.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.security.Key;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for creating, validating, and extracting claims from JWT tokens.
 * It uses the SECRET_KEY defined in JwtConstant for signing and verification.
 */
@Component
@Slf4j
public class JwtUtil {

    private Key key;

    // Token expiration time: 2 hours
    private final long EXPIRATION_TIME = TimeUnit.HOURS.toMillis(2);

    /**
     * Initializes the signing key using the secret string. This runs once after
     * the bean is constructed.
     */
    @PostConstruct
    public void init() {
        log.info("Reached JwtUtil.init.");

        // The secret key must be Base64-decoded and used to initialize an HMAC-SHA key
        // Reads the hardcoded secret key from the JwtConstant class.
        // This bypasses the @Value injection failure you experienced earlier.
        String secret = JwtConstant.SECRET_KEY;
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generates a new JWT token signed with the configured secret key.
     * This token should be returned to the client upon successful login.
     *
     * @param subject The user identifier (e.g., username or user ID) to embed as the subject.
     * @return The signed JWT string.
     */
    public String generateToken(String subject) {
        log.info("Reached JwtUtil.generateToken.");

        long now = System.currentTimeMillis();
        Date issueDate = new Date(now);
        Date expirationDate = new Date(now + EXPIRATION_TIME);

        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(issueDate)
                .setExpiration(expirationDate)
                // The signature is added here, ensuring the token cannot be tampered with
                .signWith(key)
                .compact();
    }

    /**
     * Validates the integrity and expiration of the given JWT token.
     * Note: In a Gateway scenario, this method is used by the JwtGlobalFilter.
     *
     * @param token The JWT string to validate.
     */
    public void validateToken(String token) {
        log.info("Reached JwtUtil.validateToken.");

        try {
            // Parsing the JWS will automatically check the signature and expiration time
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
        } catch (JwtException e) {
            // Throws runtime exception for expired, malformed, or invalid signature
            throw new RuntimeException("JWT Validation failed: " + e.getMessage());
        }
    }

    /**
     * Extracts the claims (payload) from a valid JWT token.
     *
     * @param token The JWT string.
     * @return The claims object (payload) if the token is valid.
     */
    public Claims getClaims(String token) {
        log.info("Reached JwtUtil.getClaims.");

        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }
}