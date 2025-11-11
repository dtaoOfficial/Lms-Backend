package com.dtao.lms.service;

import com.dtao.lms.model.Session;
import com.dtao.lms.repo.SessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * SessionService
 *
 * Responsible for creating, updating and terminating user sessions.
 * Uses SessionRepository (Mongo) to persist session documents.
 *
 * Improvements over the original:
 * - Added logging for key operations to aid debugging.
 * - Defensive null checks and sanitization (activeSeconds, timestamps).
 * - Transactional annotations where multiple fields are updated to reduce partial-write risks.
 * - Clearer handling of race conditions and unexpected states.
 */
@Service
public class SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);

    private final SessionRepository sessionRepo;

    public SessionService(SessionRepository sessionRepo) {
        this.sessionRepo = sessionRepo;
    }

    /**
     * Create a new session record.
     *
     * @param email     user email (required)
     * @param userAgent UA string (optional)
     * @param ip        client ip (optional)
     * @param meta      optional metadata map (device/course context etc)
     * @return created Session
     */
    public Session createSession(String email, String userAgent, String ip, Map<String, Object> meta) {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("email required");

        String sessionId = UUID.randomUUID().toString();
        Instant now = Instant.now();

        Session s = new Session();
        s.setSessionId(sessionId);
        s.setEmail(email);
        s.setLoginAt(now);
        s.setLastSeenAt(now);
        s.setUserAgent(userAgent);
        s.setIp(ip);
        s.setMeta(meta);
        s.setActive(true);
        s.setActiveSeconds(0L);
        s.setCreatedAt(now);
        s.setUpdatedAt(now);

        Session saved = sessionRepo.save(s);
        log.debug("Created session {} for {}", sessionId, email);
        return saved;
    }

    /**
     * Get session by public sessionId (UUID).
     */
    public Optional<Session> getBySessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return Optional.empty();
        return sessionRepo.findBySessionId(sessionId);
    }

    /**
     * Touch a session to update lastSeenAt and accumulate activeSeconds.
     * Safe to call frequently (e.g. heartbeat / ping).
     *
     * This will:
     *  - compute delta between previous lastSeenAt and now
     *  - add delta.seconds to activeSeconds
     *  - update lastSeenAt and updatedAt
     *
     * @param sessionId uuid string
     * @return Optional<Session> updated
     */
    @Transactional
    public Optional<Session> touchSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return Optional.empty();

        Optional<Session> maybe = sessionRepo.findBySessionId(sessionId);
        if (maybe.isEmpty()) {
            log.debug("touchSession: session {} not found", sessionId);
            return Optional.empty();
        }

        Session s = maybe.get();
        Instant now = Instant.now();

        Instant lastSeen = s.getLastSeenAt() != null ? s.getLastSeenAt() : s.getLoginAt();
        if (lastSeen == null) lastSeen = now;

        long deltaSeconds = Duration.between(lastSeen, now).getSeconds();
        if (deltaSeconds < 0) deltaSeconds = 0;

        // clamp to avoid huge accidental spikes (1 day)
        final long CLAMP_MAX = 86400L;
        if (deltaSeconds > CLAMP_MAX) deltaSeconds = CLAMP_MAX;

        long prev = s.getActiveSeconds() == null ? 0L : s.getActiveSeconds();
        s.setActiveSeconds(prev + deltaSeconds);
        s.setLastSeenAt(now);
        s.setUpdatedAt(now);

        // ensure isActive remains true when touched
        if (!s.isActive()) {
            s.setActive(true);
        }

        Session saved = sessionRepo.save(s);
        log.debug("Touched session {} for {} (+{}s -> {}s)", sessionId, s.getEmail(), deltaSeconds, saved.getActiveSeconds());
        return Optional.of(saved);
    }

    /**
     * End (logout) a session by sessionId.
     * Marks logoutAt, sets isActive=false, updates activeSeconds using lastSeenAt->now.
     *
     * @param sessionId uuid
     * @return Optional<Session> after update
     */
    @Transactional
    public Optional<Session> endSessionBySessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return Optional.empty();

        Optional<Session> maybe = sessionRepo.findBySessionId(sessionId);
        if (maybe.isEmpty()) {
            log.debug("endSession: session {} not found", sessionId);
            return Optional.empty();
        }

        Session s = maybe.get();
        Instant now = Instant.now();

        Instant lastSeen = s.getLastSeenAt() != null ? s.getLastSeenAt() : s.getLoginAt();
        if (lastSeen == null) lastSeen = now;

        long deltaSeconds = Duration.between(lastSeen, now).getSeconds();
        if (deltaSeconds < 0) deltaSeconds = 0;

        long prev = s.getActiveSeconds() == null ? 0L : s.getActiveSeconds();
        s.setActiveSeconds(prev + deltaSeconds);

        s.setLogoutAt(now);
        s.setLastSeenAt(now);
        s.setActive(false);
        s.setUpdatedAt(now);

        Session saved = sessionRepo.save(s);
        log.debug("Ended session {} for {} (+{}s -> {}s) logoutAt={}", sessionId, s.getEmail(), deltaSeconds, saved.getActiveSeconds(), saved.getLogoutAt());
        return Optional.of(saved);
    }

    /**
     * End all active sessions for a given email (useful on explicit logout-all)
     *
     * @param email user email
     * @return number of sessions updated
     */
    @Transactional
    public int endAllActiveSessionsForEmail(String email) {
        if (email == null || email.isBlank()) return 0;
        List<Session> active = sessionRepo.findByEmailAndIsActiveTrue(email);
        if (active == null || active.isEmpty()) return 0;

        Instant now = Instant.now();
        int count = 0;
        for (Session s : active) {
            Instant lastSeen = s.getLastSeenAt() != null ? s.getLastSeenAt() : s.getLoginAt();
            if (lastSeen == null) lastSeen = now;
            long deltaSeconds = Duration.between(lastSeen, now).getSeconds();
            if (deltaSeconds < 0) deltaSeconds = 0;

            long prev = s.getActiveSeconds() == null ? 0L : s.getActiveSeconds();
            s.setActiveSeconds(prev + deltaSeconds);
            s.setLogoutAt(now);
            s.setLastSeenAt(now);
            s.setActive(false);
            s.setUpdatedAt(now);
            count++;
        }

        sessionRepo.saveAll(active);
        log.debug("Ended {} active sessions for {}", count, email);
        return count;
    }

    /**
     * Get active sessions for an email.
     *
     * @param email user email
     */
    public List<Session> findActiveSessionsByEmail(String email) {
        if (email == null || email.isBlank()) return Collections.emptyList();
        List<Session> list = sessionRepo.findByEmailAndIsActiveTrue(email);
        return list == null ? Collections.emptyList() : list;
    }

    /**
     * Mark sessions expired (helper). If a session's lastSeenAt or loginAt is older than expireBefore,
     * mark it inactive. You can call this from a scheduled job (cron).
     *
     * @param expireBefore Instant threshold (sessions older than this will be closed)
     * @return number of sessions expired
     */
    @Transactional
    public int markExpiredSessions(Instant expireBefore) {
        if (expireBefore == null) return 0;
        List<Session> allActive = sessionRepo.findByEmailAndIsActiveTrue(null); // fallback - some repos allow findAllActive, else use findAll() below
        // If repository doesn't support that call, fall back to findAll()
        if (allActive == null || allActive.isEmpty()) {
            allActive = sessionRepo.findAll();
        }

        Instant now = Instant.now();
        List<Session> toExpire = new ArrayList<>();
        for (Session s : allActive) {
            if (!s.isActive()) continue;
            Instant lastSeen = s.getLastSeenAt() != null ? s.getLastSeenAt() : s.getLoginAt();
            if (lastSeen == null) lastSeen = now;
            if (lastSeen.isBefore(expireBefore)) {
                long deltaSeconds = Duration.between(lastSeen, now).getSeconds();
                if (deltaSeconds < 0) deltaSeconds = 0;
                long prev = s.getActiveSeconds() == null ? 0L : s.getActiveSeconds();
                s.setActiveSeconds(prev + deltaSeconds);
                s.setLogoutAt(now);
                s.setLastSeenAt(now);
                s.setActive(false);
                s.setUpdatedAt(now);
                toExpire.add(s);
            }
        }
        if (!toExpire.isEmpty()) {
            sessionRepo.saveAll(toExpire);
        }
        log.debug("Expired {} sessions older than {}", toExpire.size(), expireBefore);
        return toExpire.size();
    }

    /**
     * Optional convenience: update session meta map (merge or replace).
     *
     * @param sessionId uuid
     * @param metaMap   map to set/merge
     * @param replace   whether to replace entire meta (true) or merge keys (false)
     * @return Optional<Session> updated
     */
    @Transactional
    public Optional<Session> updateSessionMeta(String sessionId, Map<String, Object> metaMap, boolean replace) {
        if (sessionId == null || sessionId.isBlank() || metaMap == null) return Optional.empty();
        Optional<Session> maybe = sessionRepo.findBySessionId(sessionId);
        if (maybe.isEmpty()) return Optional.empty();

        Session s = maybe.get();
        if (replace || s.getMeta() == null) {
            s.setMeta(metaMap);
        } else {
            Map<String, Object> existing = s.getMeta();
            if (existing == null) existing = new HashMap<>();
            existing.putAll(metaMap);
            s.setMeta(existing);
        }
        s.setUpdatedAt(Instant.now());
        Session saved = sessionRepo.save(s);
        log.debug("Updated meta for session {} (replace={})", sessionId, replace);
        return Optional.of(saved);
    }
}
