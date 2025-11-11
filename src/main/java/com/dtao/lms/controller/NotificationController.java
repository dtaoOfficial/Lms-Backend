package com.dtao.lms.controller;

import com.dtao.lms.dto.NotificationResponse;
import com.dtao.lms.model.Notification;
import com.dtao.lms.repo.NotificationRepository;
import com.dtao.lms.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(
        origins = "${APP_CORS_ALLOWED_ORIGINS:http://localhost:3000}",
        allowCredentials = "true",
        allowedHeaders = "*"
)public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    // 🆕 Added this line — injects the Mongo repository
    @Autowired
    private NotificationRepository notificationRepository;

    /**
     * 📨 Get all notifications for current authenticated user
     */
    @GetMapping("")
    public ResponseEntity<?> getMyNotifications(Authentication auth) {
        try {
            if (auth == null || auth.getName() == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }
            String email = auth.getName();
            List<Notification> list = notificationService.getUserNotifications(email);
            List<NotificationResponse> resp = list.stream()
                    .map(NotificationResponse::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * ✅ Mark a single notification as read
     */
    @PatchMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable String id) {
        try {
            notificationService.markAsRead(id);
            return ResponseEntity.ok(Map.of("message", "Notification marked as read"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to mark as read"));
        }
    }

    /**
     * ✅ Mark all notifications as read for the logged-in user
     */
    @PatchMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(Authentication auth) {
        try {
            if (auth == null || auth.getName() == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }
            String email = auth.getName();
            notificationService.markAllAsRead(email);
            return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to mark all as read"));
        }
    }

    /**
     * 🚀 Send notifications (Admin Trigger)
     * Can send to:
     * - "ALL" → all users
     * - "STUDENT" → all students
     * - "TEACHER" → all teachers
     * - "CUSTOM" → one specific email
     */
    @PostMapping("/send")
    public ResponseEntity<?> sendNotification(@RequestBody Map<String, Object> body) {
        try {
            String target = (String) body.get("email");  // "ALL", "STUDENT", "TEACHER" or user email
            String title = (String) body.get("title");
            String message = (String) body.get("message");
            String type = (String) body.getOrDefault("type", "ADMIN");

            if (title == null || message == null || target == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing required fields"));
            }

            notificationService.sendNotification(target, title, message, type);
            return ResponseEntity.ok(Map.of(
                    "message", "Notification(s) sent successfully",
                    "target", target,
                    "type", type
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 🔁 Optional WebSocket test endpoint
     */
    @MessageMapping("/notifications")
    public void handleSocketMessage(Map<String, Object> payload) {
        System.out.println("Received WS message: " + payload);
    }

    /**
     * 🧾 Admin: Get all notifications (history view)
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllNotifications() {
        try {
            List<Notification> list = notificationRepository.findAll();
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to load notifications"));
        }
    }

    /**
     * ✏️ Update an existing notification (Admin only)
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateNotification(@PathVariable String id, @RequestBody Map<String, Object> body) {
        try {
            Notification n = notificationRepository.findById(id).orElse(null);
            if (n == null) {
                return ResponseEntity.status(404).body(Map.of("error", "Notification not found"));
            }

            String title = (String) body.get("title");
            String message = (String) body.get("message");
            String type = (String) body.getOrDefault("type", n.getType());

            if (title != null) n.setTitle(title);
            if (message != null) n.setMessage(message);
            if (type != null) n.setType(type);

            notificationRepository.save(n);
            return ResponseEntity.ok(Map.of("message", "Notification updated successfully", "data", n));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * ❌ Delete a single notification
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNotification(@PathVariable String id) {
        try {
            if (!notificationRepository.existsById(id)) {
                return ResponseEntity.status(404).body(Map.of("error", "Notification not found"));
            }
            notificationRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Notification deleted successfully"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to delete notification"));
        }
    }

    /**
     * 🧹 Delete all notifications (optional)
     */
    @DeleteMapping("/clear")
    public ResponseEntity<?> clearAllNotifications() {
        try {
            notificationRepository.deleteAll();
            return ResponseEntity.ok(Map.of("message", "All notifications cleared"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to clear notifications"));
        }
    }


}
