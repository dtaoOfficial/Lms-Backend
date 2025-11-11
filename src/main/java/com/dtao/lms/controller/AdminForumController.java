package com.dtao.lms.controller;

import com.dtao.lms.model.Report;
import com.dtao.lms.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/forum")
@CrossOrigin(
        origins = "${APP_CORS_ALLOWED_ORIGINS:http://localhost:3000}",
        allowCredentials = "true",
        allowedHeaders = "*"
)
public class AdminForumController {

    private final ReportService reportService;

    // ✅ Optional: to log or debug which origins are allowed at runtime
    @Value("${APP_CORS_ALLOWED_ORIGINS:http://localhost:3000}")
    private String allowedOrigins;

    @Autowired
    public AdminForumController(ReportService reportService) {
        this.reportService = reportService;
    }

    // ✅ 1. List All Reports (forum or video)
    @GetMapping("/reports")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllReports(@RequestParam(required = false) String status,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        List<Report> reports = reportService.listReports(status, page, size);
        long total = reports.size();
        return ResponseEntity.ok(Map.of(
                "total", total,
                "items", reports
        ));
    }

    // ✅ 2. Update Report Status (REVIEWED / CLOSED)
    @PatchMapping("/report/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateReportStatus(@PathVariable String id,
                                                @RequestBody Map<String, String> body,
                                                Principal principal) {
        String status = body.getOrDefault("status", "CLOSED").toUpperCase();
        String note = body.getOrDefault("adminNote", "");
        String adminEmail = principal != null ? principal.getName() : "system@admin";

        Optional<Report> updated = reportService.updateReportStatus(id, adminEmail, status, note);
        if (updated.isPresent()) {
            return ResponseEntity.ok(updated.get());
        }
        return ResponseEntity.notFound().build();
    }

    // ✅ 3. Delete Report (optional cleanup)
    @DeleteMapping("/report/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteReport(@PathVariable String id) {
        reportService.deleteReportById(id);
        return ResponseEntity.ok(Map.of("message", "Report deleted successfully"));
    }
}
