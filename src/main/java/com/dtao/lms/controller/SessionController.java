package com.dtao.lms.controller;

import com.dtao.lms.model.Session;
import com.dtao.lms.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * SessionController
 *
 * Endpoints to create/touch/end sessions. Designed to be called by frontend
 * on login, periodic heartbeat (player), and logout.
 *
 * Minimal, defensive validation + logging added to help debug missing touches/creates.
 */
@RestController
@RequestMapping("/api/sessions")
@CrossOrigin(
        origins = "${APP_CORS_ALLOWED_ORIGINS:http://localhost:3000}",
        allowCredentials = "true",
        allowedHeaders = "*"
)
public class SessionController {

    private static final Logger log = LoggerFactory.getLogger(SessionController.class);

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    private String currentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        return auth.getName();
    }

    /**
     * Create a new session for current authenticated user.
     * Frontend should call this on login (or whenever you want to start tracking).
     * Body (optional): { "meta": { ... } }
     */
    @PostMapping
    public ResponseEntity<Object> createSession(HttpServletRequest request, @RequestBody(required = false) Map<String, Object> body) {
        String email = currentUserEmail();
        if (email == null) {
            log.warn("createSession: unauthorized attempt");
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        try {
            String ua = request.getHeader("User-Agent");
            String ip = request.getRemoteAddr();

            Map<String, Object> meta = null;
            if (body != null && body.get("meta") instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> mm = (Map<String, Object>) body.get("meta");
                meta = mm;
            }

            Session s = sessionService.createSession(email, ua, ip, meta);
            log.debug("Created session {} for {}", s.getSessionId(), email);
            return ResponseEntity.status(201).body(s);
        } catch (Exception ex) {
            log.error("createSession failed for {}: {}", email, ex.getMessage(), ex);
            return ResponseEntity.status(500).body(Map.of("error", "Could not create session"));
        }
    }

    /**
     * End (logout) a session by sessionId.
     * The sessionId is the public UUID string stored in Session.sessionId.
     */
    @PostMapping("/{sessionId}/end")
    public ResponseEntity<Object> endSession(@PathVariable("sessionId") String sessionId) {
        String email = currentUserEmail();
        if (email == null) {
            log.warn("endSession: unauthorized attempt");
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        if (sessionId == null || sessionId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "sessionId required"));
        }

        try {
            Optional<Session> maybe = sessionService.getBySessionId(sessionId);
            if (maybe.isEmpty()) {
                log.debug("endSession: session {} not found (requested by {})", sessionId, email);
                return ResponseEntity.status(404).body(Map.of("error", "Session not found"));
            }

            Session s = maybe.get();
            if (!email.equals(s.getEmail())) {
                log.warn("endSession: forbidden - user {} tried to end session owned by {}", email, s.getEmail());
                return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
            }

            Optional<Session> ended = sessionService.endSessionBySessionId(sessionId);
            if (ended.isPresent()) {
                log.debug("Ended session {} for {}", sessionId, email);
                return ResponseEntity.ok(ended.get());
            } else {
                log.error("endSession: could not end session {} for {}", sessionId, email);
                return ResponseEntity.status(500).body(Map.of("error", "Could not end session"));
            }
        } catch (Exception ex) {
            log.error("endSession failed for {} session {}: {}", email, sessionId, ex.getMessage(), ex);
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Touch / heartbeat endpoint — updates lastSeenAt and activeSeconds.
     * Call periodically from frontend (e.g. every 20-60s) while user is active (player running).
     */
    @PostMapping("/{sessionId}/touch")
    public ResponseEntity<Object> touchSession(@PathVariable("sessionId") String sessionId) {
        String email = currentUserEmail();
        if (email == null) {
            log.warn("touchSession: unauthorized attempt");
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        if (sessionId == null || sessionId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "sessionId required"));
        }

        try {
            Optional<Session> maybe = sessionService.getBySessionId(sessionId);
            if (maybe.isEmpty()) {
                log.debug("touchSession: session {} not found (requested by {})", sessionId, email);
                return ResponseEntity.status(404).body(Map.of("error", "Session not found"));
            }

            Session s = maybe.get();
            if (!email.equals(s.getEmail())) {
                log.warn("touchSession: forbidden - user {} tried to touch session owned by {}", email, s.getEmail());
                return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
            }

            Optional<Session> updated = sessionService.touchSession(sessionId);
            if (updated.isPresent()) {
                log.debug("Touched session {} for {}", sessionId, email);
                return ResponseEntity.ok(updated.get());
            } else {
                log.error("touchSession: could not update session {} for {}", sessionId, email);
                return ResponseEntity.status(500).body(Map.of("error", "Could not touch session"));
            }
        } catch (Exception ex) {
            log.error("touchSession failed for {} session {}: {}", email, sessionId, ex.getMessage(), ex);
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Update session meta (merge by default).
     * Request body: { "meta": { ... }, "replace": false }
     */
    @PatchMapping("/{sessionId}/meta")
    public ResponseEntity<Object> updateSessionMeta(@PathVariable("sessionId") String sessionId,
                                                    @RequestBody Map<String, Object> body) {
        String email = currentUserEmail();
        if (email == null) {
            log.warn("updateSessionMeta: unauthorized attempt");
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        if (sessionId == null || sessionId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "sessionId required"));
        }
        if (body == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "request body required"));
        }

        try {
            Optional<Session> maybe = sessionService.getBySessionId(sessionId);
            if (maybe.isEmpty()) {
                log.debug("updateSessionMeta: session {} not found (requested by {})", sessionId, email);
                return ResponseEntity.status(404).body(Map.of("error", "Session not found"));
            }

            Session s = maybe.get();
            if (!email.equals(s.getEmail())) {
                log.warn("updateSessionMeta: forbidden - user {} tried to update session owned by {}", email, s.getEmail());
                return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
            }

            Object metaObj = body.get("meta");
            boolean replace = Boolean.TRUE.equals(body.get("replace"));
            if (!(metaObj instanceof Map)) {
                return ResponseEntity.badRequest().body(Map.of("error", "meta must be an object"));
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> metaMap = (Map<String, Object>) metaObj;

            Optional<Session> updated = sessionService.updateSessionMeta(sessionId, metaMap, replace);
            if (updated.isPresent()) {
                log.debug("Updated meta for session {} by {}", sessionId, email);
                return ResponseEntity.ok(updated.get());
            } else {
                log.error("updateSessionMeta: could not update meta for session {} by {}", sessionId, email);
                return ResponseEntity.status(500).body(Map.of("error", "Could not update meta"));
            }
        } catch (Exception ex) {
            log.error("updateSessionMeta failed for {} session {}: {}", email, sessionId, ex.getMessage(), ex);
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * List active sessions for current user.
     */
    @GetMapping("/active")
    public ResponseEntity<Object> listActiveSessionsForCurrentUser() {
        String email = currentUserEmail();
        if (email == null) {
            log.warn("listActiveSessionsForCurrentUser: unauthorized attempt");
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        try {
            List<Session> list = sessionService.findActiveSessionsByEmail(email);
            return ResponseEntity.ok(list);
        } catch (Exception ex) {
            log.error("listActiveSessionsForUser failed for {}: {}", email, ex.getMessage(), ex);
            return ResponseEntity.status(500).body(Map.of("error", "Could not list active sessions"));
        }
    }

    /**
     * Get a session by sessionId (only owner may fetch).
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<Object> getBySessionId(@PathVariable("sessionId") String sessionId) {
        String email = currentUserEmail();
        if (email == null) {
            log.warn("getBySessionId: unauthorized attempt");
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        if (sessionId == null || sessionId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "sessionId required"));
        }

        try {
            Optional<Session> maybe = sessionService.getBySessionId(sessionId);
            if (maybe.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "Session not found"));
            }
            if (!email.equals(maybe.get().getEmail())) {
                return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
            }

            return ResponseEntity.ok(maybe.get());
        } catch (Exception ex) {
            log.error("getBySessionId failed for {} session {}: {}", email, sessionId, ex.getMessage(), ex);
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }
}
