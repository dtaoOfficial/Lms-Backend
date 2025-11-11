package com.dtao.lms.controller;

import com.dtao.lms.dto.StudentDashboardStatsResponse;
import com.dtao.lms.security.CustomUserDetails;
import com.dtao.lms.service.StudentDashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student/dashboard")
@CrossOrigin(
        origins = "${APP_CORS_ALLOWED_ORIGINS:http://localhost:3000}",
        allowCredentials = "true",
        allowedHeaders = "*"
)
public class StudentDashboardController {

    private static final Logger log = LoggerFactory.getLogger(StudentDashboardController.class);

    @Autowired
    private StudentDashboardService studentDashboardService;

    @GetMapping
    public ResponseEntity<StudentDashboardStatsResponse> getStudentDashboardStats(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        if (currentUser == null || currentUser.getEmail() == null || currentUser.getEmail().isBlank()) {
            log.warn("[Dashboard] Unauthorized request — missing user or email");
            return ResponseEntity.status(401).build();
        }

        try {
            StudentDashboardStatsResponse stats = studentDashboardService.getStudentDashboardStats(currentUser.getEmail());
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("[Dashboard] Failed to load stats for {}", currentUser.getEmail(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
