package com.dtao.lms.controller;

import com.dtao.lms.model.Enrollment;
import com.dtao.lms.service.EnrollmentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/enrollments")
@CrossOrigin(origins = "${APP_CORS_ALLOWED_ORIGINS:http://localhost:3000}", allowCredentials = "true")
public class AdminEnrollmentController {

    private final EnrollmentService enrollmentService;

    // ✅ Optional: For debug logging to confirm which origins are loaded
    @Value("${APP_CORS_ALLOWED_ORIGINS:http://localhost:3000}")
    private String allowedOrigins;

    public AdminEnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    // GET /api/admin/enrollments?status=PENDING
    @GetMapping("")
    public ResponseEntity<?> list(@RequestParam(required = false) String status) {
        try {
            List<Enrollment> list;
            if (status == null || status.isBlank()) {
                // return pending by default
                list = enrollmentService.getEnrollmentsByStatus("PENDING");
            } else {
                list = enrollmentService.getEnrollmentsByStatus(status.toUpperCase());
            }

            return ResponseEntity.ok(list);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Server error"));
        }
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable String id, @RequestBody(required = false) Map<String, String> body) {
        try {
            String note = body == null ? null : body.get("note");

            // get current authenticated admin email (use auth.getName() as fallback)
            var auth = SecurityContextHolder.getContext().getAuthentication();
            String adminEmail = (auth != null && auth.getName() != null) ? auth.getName() : null;

            Enrollment e = enrollmentService.approveEnrollment(id, note, adminEmail);
            return ResponseEntity.ok(e);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Server error"));
        }
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable String id, @RequestBody(required = false) Map<String, String> body) {
        try {
            String note = body == null ? null : body.get("note");

            var auth = SecurityContextHolder.getContext().getAuthentication();
            String adminEmail = (auth != null && auth.getName() != null) ? auth.getName() : null;

            Enrollment e = enrollmentService.rejectEnrollment(id, note, adminEmail);
            return ResponseEntity.ok(e);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Server error"));
        }
    }
}
