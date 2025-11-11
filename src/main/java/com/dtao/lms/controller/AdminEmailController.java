package com.dtao.lms.controller;

import com.dtao.lms.model.EmailLog;
import com.dtao.lms.service.AdminEmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Handles admin-triggered email broadcasts and email history management.
 */
@RestController
@RequestMapping("/api/admin/email")
public class AdminEmailController {

    @Autowired
    private AdminEmailService adminEmailService;

    @PostMapping("/send")
    public ResponseEntity<?> sendEmail(@RequestBody Map<String, Object> body, Authentication auth) {
        try {
            String subject = (String) body.get("subject");
            String message = (String) body.get("message");
            String target = (String) body.getOrDefault("target", "ALL");
            List<String> customRecipients = (List<String>) body.getOrDefault("recipients", List.of());

            String adminEmail = (auth != null && auth.getName() != null) ? auth.getName() : "system@lms";

            if (subject == null || message == null || subject.isBlank() || message.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Subject and message are required."));
            }

            EmailLog log = adminEmailService.sendEmail(subject, message, target, adminEmail, customRecipients);

            // ✅ Return safely (avoid Map.of null crash)
            return ResponseEntity.ok(Map.ofEntries(
                    Map.entry("status", log != null && log.isSuccess() ? "success" : "failed"),
                    Map.entry("logId", log != null ? log.getId() : "unknown"),
                    Map.entry("recipients", log != null && log.getRecipients() != null ? log.getRecipients().size() : 0),
                    Map.entry("target", target != null ? target : "unknown"),
                    Map.entry("error", log != null && log.getErrorMessage() != null ? log.getErrorMessage() : "")
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Failed to send email: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<EmailLog>> getEmailHistory() {
        try {
            List<EmailLog> logs = adminEmailService.getAllEmailLogs();
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEmail(@PathVariable String id) {
        try {
            adminEmailService.deleteEmailLog(id);
            return ResponseEntity.ok(Map.of("deleted", true, "id", id));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to delete email log"));
        }
    }
}
