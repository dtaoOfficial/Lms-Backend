package com.dtao.lms.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Session document stored in "sessions" collection.
 *
 * Notes:
 *  - sessionId is a public UUID (indexed unique) used by clients.
 *  - activeSeconds is accumulated (not recomputed from loginAt on each read) so service methods
 *    should call {@link #addActiveSecondsSinceLastSeen()} before marking logout to accumulate time.
 */
@Document(collection = "sessions")
public class Session {

    @Id
    private String id;                // internal MongoDB _id

    @Indexed(unique = true)
    private String sessionId;         // UUID/v4 session identifier (public id)

    @Indexed
    private String email;             // user email

    private Instant loginAt;
    private Instant logoutAt;
    private Instant lastSeenAt;

    private String userAgent;
    private String ip;
    private Long activeSeconds = 0L;
    private boolean isActive = true;

    private Map<String, Object> meta = new HashMap<>(); // optional device/course info
    private Instant createdAt;
    private Instant updatedAt;
    private Instant expiresAt;        // optional TTL for session expiry

    public Session() {}

    // ----- convenience constructor -----
    public Session(String email, String userAgent, String ip) {
        this.sessionId = UUID.randomUUID().toString();
        this.email = email;
        this.loginAt = Instant.now();
        this.lastSeenAt = this.loginAt;
        this.userAgent = userAgent;
        this.ip = ip;
        this.isActive = true;
        this.activeSeconds = 0L;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // ----- getters & setters -----
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Instant getLoginAt() { return loginAt; }
    public void setLoginAt(Instant loginAt) { this.loginAt = loginAt; }

    public Instant getLogoutAt() { return logoutAt; }
    public void setLogoutAt(Instant logoutAt) { this.logoutAt = logoutAt; }

    public Instant getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(Instant lastSeenAt) { this.lastSeenAt = lastSeenAt; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public Long getActiveSeconds() { return activeSeconds == null ? 0L : activeSeconds; }
    public void setActiveSeconds(Long activeSeconds) { this.activeSeconds = activeSeconds == null ? 0L : activeSeconds; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public Map<String, Object> getMeta() { return meta; }
    public void setMeta(Map<String, Object> meta) { this.meta = meta == null ? new HashMap<>() : meta; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    // ----- helper methods -----

    /**
     * Add the seconds elapsed since lastSeenAt (or loginAt if lastSeenAt null) to activeSeconds,
     * and update lastSeenAt to now. This is useful to call on touch/end to accumulate watched time.
     *
     * Returns number of seconds added (>= 0).
     */
    public long addActiveSecondsSinceLastSeen() {
        Instant now = Instant.now();
        Instant ref = lastSeenAt != null ? lastSeenAt : loginAt != null ? loginAt : now;
        if (ref == null) ref = now;
        long delta = Duration.between(ref, now).getSeconds();
        if (delta < 0) delta = 0;
        // clamp large spikes (prevent overflow from clock jumps)
        long clampMax = 86400L;
        if (delta > clampMax) delta = clampMax;

        long prev = getActiveSeconds();
        this.activeSeconds = prev + delta;
        this.lastSeenAt = now;
        this.updatedAt = now;
        return delta;
    }

    /**
     * Mark logout: accumulate any remaining seconds since lastSeenAt/loginAt and set logoutAt/isActive.
     */
    public void markLogoutAndAccumulate() {
        long added = addActiveSecondsSinceLastSeen();
        this.logoutAt = Instant.now();
        this.isActive = false;
        this.updatedAt = Instant.now();
    }

    /**
     * Touch/heartbeat: update lastSeenAt (no accumulation here — service may call addActiveSecondsSinceLastSeen separately).
     */
    public void touch() {
        this.lastSeenAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @Override
    public String toString() {
        return "Session{" +
                "sessionId='" + sessionId + '\'' +
                ", email='" + email + '\'' +
                ", loginAt=" + loginAt +
                ", logoutAt=" + logoutAt +
                ", lastSeenAt=" + lastSeenAt +
                ", activeSeconds=" + activeSeconds +
                ", isActive=" + isActive +
                '}';
    }
}
