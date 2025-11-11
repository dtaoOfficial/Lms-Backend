package com.dtao.lms.security;

import com.dtao.lms.model.Session;
import com.dtao.lms.repo.SessionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

/**
 * ✅ JwtAuthenticationFilter
 *
 * Reads JWT from Authorization header (only), validates it via JwtTokenProvider,
 * verifies the session, loads user details, and sets the SecurityContext.
 *
 * Features:
 * - Skips public endpoints (/api/auth, /api/public, /ws)
 * - Ignores cookie JWT to avoid conflicts
 * - Logs token extraction, validation, and session details
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final SessionRepository sessionRepository;

    // 🧩 Toggle this to true if you want to skip session validation temporarily
    private static final boolean DEBUG_BYPASS_SESSION_CHECK = false;

    public JwtAuthenticationFilter(
            JwtTokenProvider tokenProvider,
            CustomUserDetailsService userDetailsService,
            SessionRepository sessionRepository) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
        this.sessionRepository = sessionRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String path = req.getRequestURI();
        log.debug("▶ [JwtAuthFilter] Incoming request: {} {}", req.getMethod(), path);

        // ✅ Allow preflight CORS requests
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            chain.doFilter(req, res);
            return;
        }

        // ✅ Skip JWT validation for public routes (auth, register, verify, etc.)
        if (isPublicEndpoint(path)) {
            log.debug("⚪ [JwtAuthFilter] Public endpoint detected — skipping JWT validation");
            chain.doFilter(req, res);
            return;
        }

        String token = getTokenFromRequest(req);
        if (!StringUtils.hasText(token)) {
            log.debug("⚪ [JwtAuthFilter] No JWT token found — skipping authentication");
            chain.doFilter(req, res);
            return;
        }

        log.debug("🟡 [JwtAuthFilter] JWT found — starting validation...");

        try {
            // ✅ Step 1: Validate token
            if (!tokenProvider.validateToken(token)) {
                log.warn("🚫 [JwtAuthFilter] Invalid or expired JWT detected.");
                clearJwtCookie(res);
                SecurityContextHolder.clearContext();
                res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
                return;
            }

            // ✅ Step 2: Extract username + sessionId
            String username = tokenProvider.getUsernameFromToken(token);
            String sessionId = tokenProvider.getSessionIdFromToken(token);

            log.info("🧠 [JwtAuthFilter] Token valid for user={} | sessionId={}", username, sessionId);

            // ✅ Step 3: Session validation (optional bypass for local debug)
            if (!DEBUG_BYPASS_SESSION_CHECK && StringUtils.hasText(sessionId)) {
                Optional<Session> maybeSession = sessionRepository.findBySessionId(sessionId);

                if (maybeSession.isEmpty()) {
                    log.warn("⚠️ [JwtAuthFilter] No session found for ID={} → rejecting request", sessionId);
                    clearJwtCookie(res);
                    SecurityContextHolder.clearContext();
                    res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Session not found");
                    return;
                }

                Session session = maybeSession.get();
                if (!session.isActive()) {
                    log.warn("⚠️ [JwtAuthFilter] Session inactive for user={} → rejecting request", username);
                    clearJwtCookie(res);
                    SecurityContextHolder.clearContext();
                    res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Session inactive");
                    return;
                }

                if (session.getExpiresAt() != null && session.getExpiresAt().isBefore(Instant.now())) {
                    log.warn("⚠️ [JwtAuthFilter] Session expired at {} → rejecting request", session.getExpiresAt());
                    clearJwtCookie(res);
                    SecurityContextHolder.clearContext();
                    res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Session expired");
                    return;
                }

                log.debug("✅ [JwtAuthFilter] Session active and valid for {}", username);
            } else if (DEBUG_BYPASS_SESSION_CHECK) {
                log.warn("🧩 [JwtAuthFilter] DEBUG MODE — session check bypassed for user={}", username);
            }

            // ✅ Step 4: Load user details
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            if (userDetails == null) {
                log.warn("⚠️ [JwtAuthFilter] UserDetails not found for {}", username);
                clearJwtCookie(res);
                SecurityContextHolder.clearContext();
                res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User not found");
                return;
            }

            // ✅ Step 5: Set authentication in context
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.info("✅ [JwtAuthFilter] Auth established for {}", username);

        } catch (Exception ex) {
            log.error("💥 [JwtAuthFilter] Exception validating JWT: {}", ex.getMessage(), ex);
            SecurityContextHolder.clearContext();
            clearJwtCookie(res);
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized - filter error");
            return;
        }

        chain.doFilter(req, res);
    }

    /**
     * ✅ Helper — define public endpoints that skip JWT validation
     */
    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/api/auth/") ||
                path.startsWith("/api/public/") ||
                path.startsWith("/ws/");
    }

    /**
     * Extract JWT token from Authorization header only.
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        log.debug("📦 [JwtAuthFilter] Raw Authorization header: {}", bearer);
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }

    /**
     * Clears invalid JWT cookie (harmless since JWT cookie not used anymore).
     */
    private void clearJwtCookie(HttpServletResponse res) {
        try {
            Cookie jwt = new Cookie("JWT", "");
            jwt.setHttpOnly(true);
            jwt.setPath("/");
            jwt.setMaxAge(0);
            res.addCookie(jwt);
            log.debug("🧹 [JwtAuthFilter] Cleared JWT cookie");
        } catch (Exception e) {
            log.error("💥 [JwtAuthFilter] Failed to clear cookie: {}", e.getMessage());
        }
    }
}
