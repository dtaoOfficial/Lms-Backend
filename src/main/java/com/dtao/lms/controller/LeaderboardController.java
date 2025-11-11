package com.dtao.lms.controller;

import com.dtao.lms.dto.LeaderboardResponse;
import com.dtao.lms.model.User;
import com.dtao.lms.service.LeaderboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 🏆 LeaderboardController
 * Handles course-based, global, and XP leaderboards.
 */
@RestController
@RequestMapping("/api/leaderboard")
@CrossOrigin(
        origins = "${APP_CORS_ALLOWED_ORIGINS:http://localhost:3000}",
        allowCredentials = "true",
        allowedHeaders = "*"
)
public class LeaderboardController {

    private static final Logger log = LoggerFactory.getLogger(LeaderboardController.class);
    private final LeaderboardService leaderboardService;

    // ✅ Manual constructor replacing Lombok's @RequiredArgsConstructor
    @Autowired
    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    /**
     * 📘 Get leaderboard for a specific course.
     * Example: GET /api/leaderboard/{courseId}
     */
    @GetMapping("/{courseId}")
    public ResponseEntity<LeaderboardResponse> getCourseLeaderboard(@PathVariable String courseId) {
        log.info("📘 Fetching leaderboard for courseId={}", courseId);
        LeaderboardResponse response = leaderboardService.generateLeaderboard(courseId);
        return ResponseEntity.ok(response);
    }

    /**
     * 🌍 Get global leaderboard (all students, all courses)
     * Example: GET /api/leaderboard/global
     */
    @GetMapping("/global")
    public ResponseEntity<LeaderboardResponse> getGlobalLeaderboard() {
        log.info("🌍 Fetching global leaderboard");
        LeaderboardResponse response = leaderboardService.generateGlobalLeaderboard();
        return ResponseEntity.ok(response);
    }

    /**
     * ⚡ XP Leaderboard (used by Admin Gamification Page)
     * Example: GET /api/leaderboard/xp
     *
     * Returns a simplified flat list of all students ranked by XP.
     */
    @GetMapping("/xp")
    public ResponseEntity<?> getGlobalXPLeaderboard() {
        try {
            log.info("⚡ Fetching XP leaderboard for Admin Gamification Page");

            // Fetch all students
            List<User> students = leaderboardService.getAllStudents();
            List<Map<String, Object>> result = new ArrayList<>();

            for (User s : students) {
                int xp = leaderboardService.getXpForStudent(s.getEmail());
                int level = (xp / 100) + 1;
                String badge = xp >= 500 ? "Pro Learner 🥇" :
                        xp >= 200 ? "Active Learner 🥈" :
                                "New Learner 🐣";

                Map<String, Object> map = new LinkedHashMap<>();
                map.put("email", s.getEmail());
                map.put("xp", xp);
                map.put("level", level);
                map.put("badge", badge);
                map.put("name", s.getName());
                result.add(map);
            }

            // Sort by XP descending
            result.sort((a, b) -> Integer.compare((int) b.get("xp"), (int) a.get("xp")));

            // Assign rank
            int rank = 1;
            for (Map<String, Object> m : result) {
                m.put("rank", rank++);
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("💥 Failed to fetch XP leaderboard: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to fetch XP leaderboard"));
        }
    }

    /**
     * 🧠 Exam Leaderboard (marks/percentage-based)
     * Example: GET /api/leaderboard/exam/{examId}
     */
    @GetMapping("/exam/{examId}")
    public ResponseEntity<LeaderboardResponse> getExamLeaderboard(@PathVariable String examId) {
        log.info("📊 Fetching leaderboard for examId={}", examId);
        LeaderboardResponse response = leaderboardService.generateExamLeaderboard(examId);
        return ResponseEntity.ok(response);
    }

}
