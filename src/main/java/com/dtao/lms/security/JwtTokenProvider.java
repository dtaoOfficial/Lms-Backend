package com.dtao.lms.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * ✅ JwtTokenProvider
 * Generates, validates, and parses JWT tokens with ROLE_ authorities.
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_SESSION = "sessionId";
    public static final String CLAIM_AUTHORITIES = "authorities";

    @Value("${jwt.secret:${JWT_SECRET:}}")
    private String jwtSecretEnv;


    @Value("${jwt.expiration.seconds:900}")
    private long jwtExpirationSecondsDefault;

    @Value("${jwt.rememberme.seconds:1209600}")
    private long jwtRememberMeSecondsDefault;

    @Value("${jwt.issuer:lms-api}")
    private String issuer;

    private Key key;
    private String loadedSecretPreview;

    @PostConstruct
    public void init() {
        String secret = jwtSecretEnv;
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("Missing jwt.secret. Please configure in environment or application.properties");
        }

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("jwt.secret must be at least 256 bits (32 bytes) for HS256");
        }

        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.loadedSecretPreview = secret.length() > 10
                ? secret.substring(0, 10) + "***** (" + keyBytes.length + " bytes)"
                : secret + " (" + keyBytes.length + " bytes)";

        log.info("[JwtTokenProvider] ✅ JWT initialized successfully.");
        log.info("    Issuer: {}", issuer);
        log.info("    Expiry: {}s | RememberMe: {}s", jwtExpirationSecondsDefault, jwtRememberMeSecondsDefault);
    }

    // --------------------------------------------------------------------
    // 🔑 Token Generation
    // --------------------------------------------------------------------
    public String generateToken(Authentication authentication, boolean rememberMe) {
        String username = authentication == null ? null : authentication.getName();
        return generateToken(username, null, null, rememberMe);
    }

    public String generateToken(String email, String role, String sessionId, boolean rememberMe) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email/subject cannot be null or blank when generating token");
        }

        long now = System.currentTimeMillis();
        long ttl = (rememberMe ? jwtRememberMeSecondsDefault : jwtExpirationSecondsDefault) * 1000L;
        Date issued = new Date(now);
        Date expiry = new Date(now + ttl);

        String authority = "ROLE_" + (role == null ? "STUDENT" : role.toUpperCase());

        JwtBuilder builder = Jwts.builder()
                .setSubject(email)
                .setIssuer(issuer)
                .setIssuedAt(issued)
                .setExpiration(expiry)
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_AUTHORITIES, List.of(authority))
                .signWith(key, SignatureAlgorithm.HS256);

        if (sessionId != null) {
            builder.claim(CLAIM_SESSION, sessionId);
        }

        log.debug("[JwtTokenProvider] Generated token for {} with authorities {}", email, authority);
        return builder.compact();
    }

    // --------------------------------------------------------------------
    // ✅ Token Validation
    // --------------------------------------------------------------------
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException ex) {
            log.warn("[JwtTokenProvider] Token expired at {}", ex.getClaims().getExpiration());
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("[JwtTokenProvider] Invalid JWT: {}", ex.getMessage());
        }
        return false;
    }

    // --------------------------------------------------------------------
    // 🧠 Claim Extraction
    // --------------------------------------------------------------------
    private Claims parseClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }

    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    public String getRoleFromToken(String token) {
        Object r = parseClaims(token).get(CLAIM_ROLE);
        return r == null ? null : r.toString();
    }

    public List<String> getAuthoritiesFromToken(String token) {
        Object a = parseClaims(token).get(CLAIM_AUTHORITIES);
        if (a instanceof List<?>) {
            return ((List<?>) a).stream().map(Object::toString).toList();
        }
        String role = getRoleFromToken(token);
        return List.of("ROLE_" + (role == null ? "STUDENT" : role.toUpperCase()));
    }

    public String getSessionIdFromToken(String token) {
        Object s = parseClaims(token).get(CLAIM_SESSION);
        return s == null ? null : s.toString();
    }

    public long getIssuedAtFromToken(String token) {
        Date i = parseClaims(token).getIssuedAt();
        return i == null ? 0L : i.getTime() / 1000L;
    }

    public long getExpiryFromToken(String token) {
        Date e = parseClaims(token).getExpiration();
        return e == null ? 0L : e.getTime() / 1000L;
    }

    public long getExpiresInSeconds(boolean rememberMe) {
        return rememberMe ? jwtRememberMeSecondsDefault : jwtExpirationSecondsDefault;
    }

    // --------------------------------------------------------------------
    // 🧩 Debug Accessors
    // --------------------------------------------------------------------
    public String getLoadedSecretPreview() { return loadedSecretPreview; }
    public long getJwtExpirationSecondsDefault() { return jwtExpirationSecondsDefault; }
    public long getJwtRememberMeSecondsDefault() { return jwtRememberMeSecondsDefault; }
    public String getIssuer() { return issuer; }
}

/**
 * ✅ Debug Controller
 * Lets you verify JWT configuration easily from Postman.
 */
@RestController
class JwtDebugController {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtDebugController(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @GetMapping("/api/debug/jwt-env")
    public Map<String, Object> getJwtDebugInfo() {
        return Map.of(
                "issuer", jwtTokenProvider.getIssuer(),
                "secretPreview", jwtTokenProvider.getLoadedSecretPreview(),
                "expirySeconds", jwtTokenProvider.getJwtExpirationSecondsDefault(),
                "rememberMeSeconds", jwtTokenProvider.getJwtRememberMeSecondsDefault()
        );
    }
}
