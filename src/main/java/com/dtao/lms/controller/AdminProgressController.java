package com.dtao.lms.controller;

import com.dtao.lms.dto.AdminProgressSummary;
import com.dtao.lms.service.AdminAnalyticsService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 🧠 AdminProgressController
 * Provides student progress analytics for admin dashboard
 */
@RestController
@RequestMapping("/api/admin/analytics")
@CrossOrigin(
        origins = "${APP_CORS_ALLOWED_ORIGINS:http://localhost:3000}",
        allowCredentials = "true",
        allowedHeaders = "*"
)
public class AdminProgressController {

    private final AdminAnalyticsService analyticsService;

    // ✅ Optional: log allowed origins for verification
    @Value("${APP_CORS_ALLOWED_ORIGINS:http://localhost:3000}")
    private String allowedOrigins;

    public AdminProgressController(AdminAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * 🧠 Returns aggregated student progress analytics.
     * Example:
     *  - totalStudents
     *  - avgProgress
     *  - per-course average completion
     *  - top students
     */
    @GetMapping("/student-progress")
    public ResponseEntity<?> getStudentProgressAnalytics() {
        try {
            AdminProgressSummary summary = analyticsService.buildStudentProgressAnalytics();
            return ResponseEntity.ok(summary);
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(
                    java.util.Map.of(
                            "error", "Could not generate analytics",
                            "reason", ex.getMessage()
                    )
            );
        }
    }

    // ✅ Optional logging for Render/Local verification
    @PostConstruct
    public void logAllowedOrigins() {
        System.out.println("🌍 [AdminProgressController] Allowed origins: " + allowedOrigins);
    }
}
