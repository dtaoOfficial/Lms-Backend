package com.dtao.lms.controller;

import com.dtao.lms.model.LeaderboardAudit;
import com.dtao.lms.service.LeaderboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/leaderboard/audit")
@CrossOrigin(
        origins = "${APP_CORS_ALLOWED_ORIGINS:http://localhost:3000}",
        allowCredentials = "true",
        allowedHeaders = "*"
)
public class LeaderboardAuditController {

    private final LeaderboardService leaderboardService;

    @Autowired
    public LeaderboardAuditController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    /**
     * 📜 Get all leaderboard reset history (audit logs)
     */
    @GetMapping("")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<LeaderboardAudit>> getAllAudits() {
        List<LeaderboardAudit> audits = leaderboardService.getAllAuditLogs();
        return ResponseEntity.ok(audits);
    }
}
