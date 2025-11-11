package com.dtao.lms.controller;

import com.dtao.lms.model.LeaderboardEntry;
import com.dtao.lms.service.LeaderboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 🏆 Admin Gamification Controller
 * Handles XP, Levels, and Badges data for admin dashboard.
 */
@RestController
@RequestMapping("/api/admin/gamification")
@CrossOrigin(
        origins = "${APP_CORS_ALLOWED_ORIGINS:http://localhost:3000}",
        allowCredentials = "true"
)
public class AdminGamificationController {

    @Autowired
    private LeaderboardService leaderboardService;

    // ✅ Optional debug info (can be logged)
    @Value("${APP_CORS_ALLOWED_ORIGINS:http://localhost:3000}")
    private String allowedOrigins;

    /**
     * ⚡ Returns XP leaderboard data (used in AdminGamificationPage)
     *
     * ✅ Non-breaking addition
     * ✅ Reuses LeaderboardService to fetch XP + Level + Badge
     * ✅ Returns simplified JSON array directly (frontend expects this)
     */
    @GetMapping("/xp")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getGamificationLeaderboard() {
        List<LeaderboardEntry> entries = leaderboardService.generateGlobalLeaderboard().getEntries();
        List<Map<String, Object>> response = new ArrayList<>();

        if (entries != null && !entries.isEmpty()) {
            for (LeaderboardEntry e : entries) {
                Map<String, Object> map = new HashMap<>();
                map.put("email", e.getEmail());
                map.put("xp", e.getXp());
                map.put("level", e.getLevel());
                map.put("badge", e.getBadge());
                map.put("rank", e.getRank());
                map.put("name", e.getName());
                response.add(map);
            }

            // Sort by XP descending
            response.sort((a, b) -> ((Integer) b.get("xp")).compareTo((Integer) a.get("xp")));
        }

        return ResponseEntity.ok(response);
    }
}
