package com.dtao.lms.service;

import com.dtao.lms.model.Notification;
import com.dtao.lms.model.User;
import com.dtao.lms.repo.NotificationRepository;
import com.dtao.lms.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * 📨 Send notifications
     * Supports:
     *  - "ALL" → all users
     *  - "STUDENT" → all students
     *  - "TEACHER" → all teachers
     *  - or any specific user email
     */
    public void sendNotification(String target, String title, String message, String type) {
        try {
            if (target == null || target.isBlank()) {
                System.err.println("[NotificationService] ❌ Target missing");
                return;
            }

            if ("ALL".equalsIgnoreCase(target)) {
                List<User> users = userRepository.findAll();
                for (User u : users) {
                    saveAndPush(u.getEmail(), title, message, type);
                }
                return;
            }

            if ("STUDENT".equalsIgnoreCase(target)) {
                List<User> students = userRepository.findByRoleIgnoreCase("STUDENT");
                for (User s : students) {
                    saveAndPush(s.getEmail(), title, message, type);
                }
                return;
            }

            if ("TEACHER".equalsIgnoreCase(target)) {
                List<User> teachers = userRepository.findByRoleIgnoreCase("TEACHER");
                for (User t : teachers) {
                    saveAndPush(t.getEmail(), title, message, type);
                }
                return;
            }

            // 🎯 If none of the above, assume a specific email target
            saveAndPush(target, title, message, type);

        } catch (Exception e) {
            System.err.println("[NotificationService] Error sending notifications: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 🧠 Save a notification and push it live via WebSocket
     */
    private void saveAndPush(String email, String title, String message, String type) {
        try {
            if (email == null || email.isBlank()) return;

            Notification n = new Notification();
            n.setUserEmail(email);
            n.setTitle(title != null ? title.trim() : "Notification");
            n.setMessage(message != null ? message.trim() : "");
            n.setType(type != null ? type.trim() : "SYSTEM");
            n.setCreatedAt(Instant.now());
            n.setRead(false);

            Notification saved = notificationRepository.save(n);

            // ✅ Push real-time via WebSocket
            try {
                messagingTemplate.convertAndSendToUser(email, "/topic/notifications", saved);
                System.out.println("[NotificationService] ✅ Live notification sent to " + email);
            } catch (Exception e) {
                System.err.println("[NotificationService] ⚠️ WebSocket push failed for " + email + ": " + e.getMessage());
            }

        } catch (Exception e) {
            System.err.println("[NotificationService] ❌ Error saving notification for " + email + ": " + e.getMessage());
        }
    }

    /**
     * 📥 Get all notifications for a user
     */
    public List<Notification> getUserNotifications(String email) {
        return notificationRepository.findByUserEmailOrderByCreatedAtDesc(email);
    }

    /**
     * 🟢 Mark a single notification as read
     */
    public void markAsRead(String id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    /**
     * 🟡 Mark all notifications for a user as read
     */
    public void markAllAsRead(String email) {
        List<Notification> list = notificationRepository.findByUserEmailOrderByCreatedAtDesc(email);
        if (list.isEmpty()) return;
        list.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(list);
    }
}
