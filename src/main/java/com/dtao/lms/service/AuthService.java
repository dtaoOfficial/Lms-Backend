package com.dtao.lms.service;

import com.dtao.lms.model.AuthToken;
import com.dtao.lms.model.Session;
import com.dtao.lms.model.User;
import com.dtao.lms.repo.AuthTokenRepository;
import com.dtao.lms.repo.SessionRepository;
import com.dtao.lms.repo.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_MINUTES = 15; // lock window after max fails
    private static final long REFRESH_TOKEN_DAYS = 30; // refresh token validity

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private AuthTokenRepository authTokenRepository;

    @Autowired
    private UserService userService; // for failed login tracking/reset

    /**
     * Create a new session.
     */
    public Session createSession(String email, String userAgent, String ip) {
        Session s = new Session();
        s.setSessionId(UUID.randomUUID().toString());
        s.setEmail(email);
        Instant now = Instant.now();
        s.setLoginAt(now);
        s.setLastSeenAt(now);
        s.setUserAgent(userAgent);
        s.setIp(ip);
        s.setActive(true);
        s.setCreatedAt(now);
        s.setUpdatedAt(now);

        return sessionRepository.save(s);
    }

    /**
     * Create a refresh token tied to a session.
     */
    public AuthToken createRefreshToken(String email, String sessionId) {
        AuthToken t = new AuthToken();
        t.setTokenId(UUID.randomUUID().toString());
        String refresh = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
        t.setToken(refresh);
        t.setEmail(email);
        t.setSessionId(sessionId);
        Instant now = Instant.now();
        t.setIssuedAt(now);
        t.setExpiresAt(now.plus(REFRESH_TOKEN_DAYS, ChronoUnit.DAYS));
        t.setRevoked(false);
        t.setCreatedAt(now);
        t.setUpdatedAt(now);
        return authTokenRepository.save(t);
    }

    /**
     * Validate a refresh token (not revoked and not expired).
     */
    public Optional<AuthToken> findValidRefresh(String token) {
        var maybe = authTokenRepository.findByToken(token);
        if (maybe.isEmpty()) return Optional.empty();
        AuthToken t = maybe.get();
        if (t.isRevoked()) return Optional.empty();
        if (t.getExpiresAt() == null || t.getExpiresAt().isBefore(Instant.now())) return Optional.empty();
        return Optional.of(t);
    }

    /**
     * Revoke a refresh token.
     */
    public void revokeRefreshToken(String token) {
        var maybe = authTokenRepository.findByToken(token);
        maybe.ifPresent(t -> {
            t.setRevoked(true);
            t.setUpdatedAt(Instant.now());
            authTokenRepository.save(t);
        });
    }

    /**
     * Called when an auth attempt fails; delegate counting/locking to UserService.
     */
    public void handleFailedAuth(String email) {
        try {
            userService.recordFailedLogin(email, MAX_FAILED_ATTEMPTS, LOCK_MINUTES);
        } catch (Exception e) {
            log.warn("Failed to record failed login for {}: {}", email, e.getMessage());
        }
    }

    /**
     * Ensure the user is not currently locked.
     */
    public void ensureNotLocked(User user) {
        if (user == null) return;
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
            throw new RuntimeException("Account locked until " + user.getLockedUntil().toString());
        }
    }

    /**
     * Cleanup after successful login (reset failed login counters).
     */
    public void successfulLoginCleanup(String email) {
        try {
            userService.resetFailedLogin(email);
        } catch (Exception e) {
            log.warn("Failed to reset failed-login for {}: {}", email, e.getMessage());
        }
    }

    /**
     * Logout a session by marking it inactive and setting logout time.
     */
    public void logoutSession(String sessionId) {
        var maybe = sessionRepository.findBySessionId(sessionId);
        maybe.ifPresent(s -> {
            s.setActive(false);
            s.setLogoutAt(Instant.now());
            s.setUpdatedAt(Instant.now());
            sessionRepository.save(s);
        });
    }
}
