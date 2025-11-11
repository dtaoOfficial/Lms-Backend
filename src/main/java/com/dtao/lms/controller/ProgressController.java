package com.dtao.lms.controller;

import com.dtao.lms.dto.CourseProgressResponse;
import com.dtao.lms.dto.VideoProgressRequest;
import com.dtao.lms.dto.VideoProgressResponse;
import com.dtao.lms.model.VideoProgress;
import com.dtao.lms.service.ProgressService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ProgressController - robust mapping of CourseProgressResponse to stable JSON
 *
 * Note: controller-level logs are kept conservative; very verbose traces are available
 * from the service when TRACE is enabled.
 */
@RestController
@RequestMapping("/api/progress")
@CrossOrigin(
        origins = "${APP_CORS_ALLOWED_ORIGINS:http://localhost:3000}",
        allowCredentials = "true",
        allowedHeaders = "*"
)
public class ProgressController {

    private static final Logger log = LoggerFactory.getLogger(ProgressController.class);

    private final ProgressService progressService;

    public ProgressController(ProgressService progressService) {
        this.progressService = progressService;
    }

    /**
     * Try to extract the user's email (or stable identifier) from the SecurityContext.
     * Handles common cases:
     *  - principal is a UserDetails (username may be email)
     *  - principal is a Jwt-like object (try to read claims via reflection)
     *  - principal might be a Map (claims)
     *  - fallback to Authentication#getName()
     *
     * Reflection avoids compile-time dependency on a specific Jwt class.
     */
    private String currentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;

        Object principal = auth.getPrincipal();
        try {
            // common case: UserDetails
            if (principal instanceof UserDetails) {
                String uname = ((UserDetails) principal).getUsername();
                if (StringUtils.hasText(uname)) return uname;
            }

            // case: principal is a Map of claims
            if (principal instanceof Map) {
                Map<?, ?> m = (Map<?, ?>) principal;
                Object email = m.get("email");
                if (email instanceof String && StringUtils.hasText((String) email)) return (String) email;
                Object sub = m.get("sub");
                if (sub instanceof String && StringUtils.hasText((String) sub)) return (String) sub;
            }

            // attempt reflection for common JWT principal shapes (avoid direct dependency)
            try {
                // try getClaim/getClaims methods
                Method getClaim = null;
                try { getClaim = principal.getClass().getMethod("getClaim", String.class); } catch (NoSuchMethodException ignore) {}
                if (getClaim != null) {
                    Object email = getClaim.invoke(principal, "email");
                    if (email instanceof String && StringUtils.hasText((String) email)) return (String) email;
                    Object sub = getClaim.invoke(principal, "sub");
                    if (sub instanceof String && StringUtils.hasText((String) sub)) return (String) sub;
                }

                Method getClaims = null;
                try { getClaims = principal.getClass().getMethod("getClaims"); } catch (NoSuchMethodException ignore) {}
                if (getClaims != null) {
                    Object claimsObj = getClaims.invoke(principal);
                    if (claimsObj instanceof Map) {
                        Map<?, ?> claims = (Map<?, ?>) claimsObj;
                        Object email = claims.get("email");
                        if (email instanceof String && StringUtils.hasText((String) email)) return (String) email;
                        Object sub = claims.get("sub");
                        if (sub instanceof String && StringUtils.hasText((String) sub)) return (String) sub;
                    }
                }
            } catch (Exception e) {
                log.debug("Reflection-based claim extraction failed: {}", e.getMessage());
            }

            // fallback: authentication name
            String name = auth.getName();
            if (StringUtils.hasText(name)) return name;
        } catch (Exception e) {
            log.debug("currentUserEmail extraction failed: {}", e.getMessage());
        }

        return null;
    }

    @GetMapping("/video/{videoId}")
    public ResponseEntity<?> getVideoProgress(@PathVariable String videoId) {
        String email = currentUserEmail();
        if (email == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        if (videoId == null || videoId.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "videoId required"));

        try {
            Optional<VideoProgress> maybe = progressService.getProgress(email, videoId);
            if (maybe.isEmpty()) {
                return ResponseEntity.ok(new VideoProgressResponse(videoId, 0.0, 0.0, false, null));
            }
            VideoProgress p = maybe.get();
            return ResponseEntity.ok(new VideoProgressResponse(p.getVideoId(), p.getLastPosition(), p.getDuration(), p.isCompleted(), p.getUpdatedAt()));
        } catch (Exception ex) {
            log.error("Error fetching video progress for {}/{} : {}", email, videoId, ex.getMessage(), ex);
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }

    @PostMapping("/video/{videoId}")
    public ResponseEntity<?> postVideoProgress(@PathVariable String videoId, @RequestBody(required = false) VideoProgressRequest req) {
        String email = currentUserEmail();
        if (email == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        if (videoId == null || videoId.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "videoId required"));
        if (req == null) return ResponseEntity.badRequest().body(Map.of("error", "request body required"));

        if (log.isTraceEnabled()) {
            log.trace("POST /api/progress/video/{} called by {} body: lastPosition={} duration={} completed={}",
                    videoId, email, req.getLastPosition(), req.getDuration(), req.getCompleted());
        }

        try {
            Double last = req.getLastPosition() == null ? null : Math.max(0.0, req.getLastPosition());
            Double dur = req.getDuration() == null ? null : Math.max(0.0, req.getDuration());
            Boolean completed = req.getCompleted();

            // For debugging: capture existing DB record at TRACE level only
            if (log.isTraceEnabled()) {
                try {
                    Optional<VideoProgress> before = progressService.getProgress(email, videoId);
                    if (before.isPresent()) {
                        log.trace("Existing progress before upsert for {}/{} : {}", email, videoId, before.get());
                    } else {
                        log.trace("No existing progress before upsert for {}/{}", email, videoId);
                    }
                } catch (Exception e) {
                    log.warn("Could not read existing progress before upsert for {}/{} : {}", email, videoId, e.getMessage(), e);
                }
            }

            VideoProgress saved = progressService.upsertProgress(email, videoId, last, dur, completed);

            if (log.isTraceEnabled()) {
                log.trace("Saved progress for {}/{} -> {}", email, videoId, saved);
            } else {
                // small, helpful debug-only message
                log.debug("Progress saved for {}/{} (completed={})", email, videoId, saved.isCompleted());
            }

            return ResponseEntity.ok(new VideoProgressResponse(saved.getVideoId(), saved.getLastPosition(), saved.getDuration(), saved.isCompleted(), saved.getUpdatedAt()));
        } catch (IllegalArgumentException iae) {
            log.warn("Bad request saving progress for {}/{} : {}", email, videoId, iae.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", iae.getMessage()));
        } catch (Exception ex) {
            log.error("Failed to save progress for {}/{} : {}", email, videoId, ex.getMessage(), ex);
            return ResponseEntity.status(500).body(Map.of("error", "Could not save progress", "reason", ex.getMessage()));
        }
    }

    @PostMapping("/video/{videoId}/complete")
    public ResponseEntity<?> completeVideo(@PathVariable String videoId) {
        String email = currentUserEmail();
        if (email == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        if (videoId == null || videoId.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "videoId required"));

        try {
            VideoProgress saved = progressService.markCompleted(email, videoId);
            log.debug("MarkComplete request for {}/{} returned completed={}", email, videoId, saved.isCompleted());
            return ResponseEntity.ok(new VideoProgressResponse(saved.getVideoId(), saved.getLastPosition(), saved.getDuration(), saved.isCompleted(), saved.getUpdatedAt()));
        } catch (Exception ex) {
            log.error("Failed to mark completed for {}/{} : {}", email, videoId, ex.getMessage(), ex);
            return ResponseEntity.status(500).body(Map.of("error", "Could not mark completed"));
        }
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<?> getCourseProgress(@PathVariable String courseId) {
        String email = currentUserEmail();
        if (email == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        if (courseId == null || courseId.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "courseId required"));

        try {
            CourseProgressResponse resp = progressService.computeCourseProgressForUser(email, courseId);
            Map<String, Object> out = new HashMap<>();

            out.put("courseId", safeInvokeString(resp, "getCourseId", "getCourse", "getId", "getCourseID"));
            Integer total = safeInvokeInteger(resp, "getTotal", "getTotalVideos", "getTotalVideoCount", "getTotalCount");
            if (total == null) total = tryCountFromListField(resp, "getAllVideoIds", "getVideoIds", "getVideos");
            if (total == null) total = 0;
            out.put("totalVideos", total);

            Integer completed = safeInvokeInteger(resp, "getCompleted", "getCompletedCount", "getVideosCompleted", "getCompletedVideos");
            if (completed == null) {
                List<?> completedList = safeInvokeList(resp, "getCompletedVideoIds", "getCompletedIds", "getCompletedVideoList", "getCompletedVideoIdsList");
                if (completedList != null) completed = completedList.size();
            }
            if (completed == null) completed = 0;
            out.put("videosCompleted", completed);

            List<?> completedIds = safeInvokeList(resp, "getCompletedVideoIds", "getCompletedIds", "getCompletedVideoList", "getCompletedVideoIdsList");
            if (completedIds == null) completedIds = Collections.emptyList();
            out.put("completedVideoIds", completedIds);

            double percent = 0.0;
            try {
                if (total > 0) percent = (completed * 100.0) / total;
            } catch (Exception ignore) { percent = 0.0; }
            out.put("percent", Math.round(percent));

            return ResponseEntity.ok(out);
        } catch (Exception ex) {
            log.error("Failed to compute course progress for {}/{} : {}", email, courseId, ex.getMessage(), ex);
            return ResponseEntity.status(500).body(Map.of("error", "Could not compute progress"));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyProgress() {
        String email = currentUserEmail();
        if (email == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));

        try {
            List<VideoProgress> list = progressService.getProgressForUser(email);
            List<VideoProgressResponse> resp = list.stream()
                    .map(p -> new VideoProgressResponse(p.getVideoId(), p.getLastPosition(), p.getDuration(), p.isCompleted(), p.getUpdatedAt()))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(resp);
        } catch (Exception ex) {
            log.error("Failed to get progress for user {} : {}", email, ex.getMessage(), ex);
            return ResponseEntity.status(500).body(Map.of("error", "Could not fetch progress"));
        }
    }

    // ----------------- Helper reflection utilities -----------------
    private static Object safeInvoke(Object target, String... methodNames) {
        if (target == null) return null;
        for (String m : methodNames) {
            try {
                Method method = target.getClass().getMethod(m);
                if (method != null) {
                    return method.invoke(target);
                }
            } catch (NoSuchMethodException ignored) {
            } catch (Exception e) {
                log.debug("safeInvoke: calling {} failed: {}", m, e.getMessage());
            }
        }
        if (target instanceof Map) return target;
        return null;
    }

    private static String safeInvokeString(Object target, String... methodNames) {
        Object o = safeInvoke(target, methodNames);
        if (o == null) return null;
        return String.valueOf(o);
    }

    private static Integer safeInvokeInteger(Object target, String... methodNames) {
        Object o = safeInvoke(target, methodNames);
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).intValue();
        try { return Integer.parseInt(String.valueOf(o)); } catch (Exception e) { return null; }
    }

    private static List<?> safeInvokeList(Object target, String... methodNames) {
        Object o = safeInvoke(target, methodNames);
        if (o == null) return null;
        if (o instanceof List) return (List<?>) o;
        if (o.getClass().isArray()) return Arrays.asList((Object[]) o);
        return null;
    }

    private static Integer tryCountFromListField(Object target, String... methodNames) {
        List<?> l = safeInvokeList(target, methodNames);
        return l == null ? null : l.size();
    }
}
