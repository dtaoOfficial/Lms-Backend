package com.dtao.lms.controller;

import com.dtao.lms.security.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(
        origins = "${APP_CORS_ALLOWED_ORIGINS:http://localhost:3000}",
        allowCredentials = "true",
        allowedHeaders = "*"
)
public class MeController {

    /**
     * GET /api/me
     * Returns currently authenticated user's basic info used by frontend (email, id, roles).
     *
     * If not authenticated -> 401.
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        Object principal = auth.getPrincipal();

        // Preferred: your CustomUserDetails which exposes getId()
        if (principal instanceof CustomUserDetails cud) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("id", cud.getId());
            // CustomUserDetails probably returns email/username via getUsername()
            resp.put("email", cud.getUsername());
            // add any extra fields your CustomUserDetails exposes (safe to add)
            resp.put("roles", cud.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority).collect(Collectors.toList()));
            return ResponseEntity.ok(resp);
        }

        // If principal is a Spring Security UserDetails (or similar)
        try {
            // many apps set principal as a String username when anonymous / stateless
            if (principal instanceof org.springframework.security.core.userdetails.UserDetails ud) {
                Map<String, Object> resp = new HashMap<>();
                resp.put("email", ud.getUsername());
                resp.put("roles", ud.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()));
                return ResponseEntity.ok(resp);
            } else if (principal instanceof String username) {
                // principal returned as username string (common with JWT setups)
                Map<String, Object> resp = new HashMap<>();
                resp.put("email", username);
                // try to provide roles from authentication
                List<String> roles = auth.getAuthorities() == null ? List.of() :
                        auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList());
                resp.put("roles", roles);
                return ResponseEntity.ok(resp);
            }
        } catch (Exception ignored) {}

        // fallback: return name and roles if possible
        Map<String, Object> resp = new HashMap<>();
        resp.put("email", auth.getName());
        resp.put("roles", auth.getAuthorities() == null ? List.of() :
                auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()));
        return ResponseEntity.ok(resp);
    }

    // ✅ NEW: Basic student overview endpoint for dashboard
    @GetMapping("/me/summary")
    public ResponseEntity<?> getUserSummary(@RequestParam String email) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("email", email);
        summary.put("timestamp", new Date());
        summary.put("message", "Student dashboard summary endpoint is active");
        return ResponseEntity.ok(summary);
    }


}
