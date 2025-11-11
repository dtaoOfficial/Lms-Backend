package com.dtao.lms.controller;

import com.dtao.lms.dto.LeaderboardAdminResponse;
import com.dtao.lms.dto.LeaderboardResetRequest;
import com.dtao.lms.model.LeaderboardEntry;
import com.dtao.lms.service.LeaderboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/leaderboard")
@CrossOrigin(
        origins = "${APP_CORS_ALLOWED_ORIGINS:http://localhost:3000}",
        allowCredentials = "true"
)
public class AdminLeaderboardController {

    private final LeaderboardService leaderboardService;

    @Value("${APP_CORS_ALLOWED_ORIGINS:http://localhost:3000}")
    private String allowedOrigins;

    @Autowired
    public AdminLeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    /**
     * 🏆 Get Global Leaderboard (Admin View)
     */
    @GetMapping("/global")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LeaderboardAdminResponse> getGlobalLeaderboardAdmin() {
        var response = new LeaderboardAdminResponse(
                "GLOBAL",
                leaderboardService.generateGlobalLeaderboard().getEntries()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 📚 Get Leaderboard for a specific course
     */
    @GetMapping("/course/{courseId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LeaderboardAdminResponse> getCourseLeaderboardAdmin(@PathVariable String courseId) {
        var response = new LeaderboardAdminResponse(
                courseId,
                leaderboardService.generateLeaderboard(courseId).getEntries()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 🔁 Reset leaderboard data (admin only)
     */
    @PostMapping("/reset")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> resetLeaderboard(@RequestBody(required = false) LeaderboardResetRequest request) {
        String scope = request != null ? request.getScope() : "GLOBAL";
        String note = request != null ? request.getNote() : "No note provided";
        Map<String, Object> result = leaderboardService.resetLeaderboardData(scope, note);
        return ResponseEntity.ok(result);
    }

    /**
     * 👑 Get Top N Global Students
     */
    @GetMapping("/top/{limit}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<LeaderboardEntry>> getTopGlobalStudents(@PathVariable int limit) {
        List<LeaderboardEntry> top = leaderboardService.getTopStudentsGlobal(limit);
        return ResponseEntity.ok(top);
    }

    /**
     * 🧠 Get Exam Leaderboard (Admin View)
     * Returns leaderboard entries for a specific exam (by examId)
     */
    @GetMapping("/exam/{examId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LeaderboardAdminResponse> getExamLeaderboardAdmin(@PathVariable String examId) {
        var response = new LeaderboardAdminResponse(
                examId,
                leaderboardService.generateExamLeaderboard(examId).getEntries()
        );
        return ResponseEntity.ok(response);
    }
}
