package com.dtao.lms.controller;

import com.dtao.lms.service.CertificateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/certificates")
@CrossOrigin(
        origins = "${APP_CORS_ALLOWED_ORIGINS:http://localhost:3000}",
        allowCredentials = "true",
        allowedHeaders = "*"
)

public class CertificateController {

    private static final Logger log = LoggerFactory.getLogger(CertificateController.class);
    private final CertificateService certificateService;

    public CertificateController(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    private String currentUserEmail() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        return auth.getName();
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generate(@RequestBody Map<String, Object> body) {
        String email = currentUserEmail();
        if (email == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));

        String courseId = body == null ? null : String.valueOf(body.get("courseId"));
        String courseTitle = body == null ? null : (String) body.getOrDefault("courseTitle", null);
        if (courseId == null) return ResponseEntity.badRequest().body(Map.of("error", "courseId required"));

        try {
            // In production, verify eligibility (completed course). For now we allow generation.
            Map<String,Object> meta = certificateService.generateCertificateForUser(email, courseId, courseTitle);
            return ResponseEntity.ok(meta);
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.badRequest().body(Map.of("error", iae.getMessage()));
        } catch (Exception e) {
            log.error("Certificate generation error for {}/{} : {}", email, courseId, e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "Could not generate certificate"));
        }
    }

    @GetMapping(value = "/{id}/download", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<?> download(@PathVariable String id) {
        try {
            byte[] pdf = certificateService.getCertificateBlob(id);
            if (pdf == null) return ResponseEntity.notFound().build();
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header("Content-Disposition", "attachment; filename=\"certificate-" + id + ".pdf\"")
                    .body(pdf);
        } catch (Exception e) {
            log.error("Certificate download failed {} : {}", id, e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "Could not download certificate"));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> myCertificates() {
        String email = currentUserEmail();
        if (email == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        try {
            var list = certificateService.listCertificatesForUser(email);
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            log.error("list certificates failed for {} : {}", email, e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "Could not list certificates"));
        }
    }



}
