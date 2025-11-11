package com.dtao.lms.service;

import com.dtao.lms.model.Exam;
import com.dtao.lms.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * EmailNotificationService
 *
 * Sends transactional and broadcast emails using Brevo API (v3).
 * Works for:
 *  - Welcome emails
 *  - New course announcements
 *  - New lesson/chapter/video notifications
 */
@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    @Value("${BREVO_API_KEY:}")
    private String brevoApiKey;

    @Value("${APP_MAIL_FROM:imsingle8688@gmail.com}")
    private String mailFrom;

    @Value("${APP_MAIL_OWNER:DTAO Admin}")
    private String ownerName;

    @Value("${APP_MAIL_COMPANY:DTAO OFFICIAL}")
    private String companyName;

    @Value("${APP_MAIL_WEBSITE:https://dtaoofficial.netlify.app/}")
    private String website;

    private static final String BREVO_URL = "https://api.brevo.com/v3/smtp/email";

    private final RestTemplate restTemplate = new RestTemplate();

    // ============================================================
    // 🔹 1. Send Welcome Email
    // ============================================================
    public void sendWelcomeEmail(User user) {
        if (user == null || user.getEmail() == null) return;

        String html = """
                <div style="font-family:Arial,sans-serif;background:#f6f9fc;padding:30px;">
                  <div style="max-width:600px;margin:auto;background:white;border-radius:10px;overflow:hidden;box-shadow:0 2px 6px rgba(0,0,0,0.1)">
                    <div style="background:#4285f4;color:white;padding:15px 20px;font-size:20px;font-weight:bold;">
                      DTAO Learning Portal
                    </div>
                    <div style="padding:25px;">
                      <h2 style="color:#4285f4;">Welcome %s 🎉</h2>
                      <p>We’re thrilled to have you on board! Start learning and explore your courses today.</p>
                      <br/>
                      <a href="%s" style="background:#4285f4;color:white;text-decoration:none;padding:10px 20px;border-radius:6px;">Visit Website</a>
                    </div>
                    <div style="text-align:center;padding:15px;font-size:12px;color:#888;background:#f7f7f7;">
                      © 2025 %s | <a href="%s" style="color:#4285f4;">Visit Website</a>
                    </div>
                  </div>
                </div>
                """.formatted(user.getName(), website, companyName, website);

        sendBrevoEmail(user.getEmail(), "🎉 Welcome to DTAO Learning Portal", html);
    }

    // ============================================================
    // 🔹 2. Send New Course Announcement
    // ============================================================
    public void sendNewCourseNotification(String courseTitle, String description, List<User> students) {
        if (students == null || students.isEmpty()) return;

        String html = """
                <div style="font-family:Arial,sans-serif;background:#f9fafb;padding:20px;">
                  <div style="max-width:600px;margin:auto;background:white;border-radius:10px;padding:25px;box-shadow:0 1px 5px rgba(0,0,0,0.1)">
                    <h2 style="color:#2563eb;">📚 New Course Added: %s</h2>
                    <p>%s</p>
                    <p>Start exploring this new course today in your LMS dashboard.</p>
                    <a href="%s" style="background:#2563eb;color:white;padding:10px 18px;text-decoration:none;border-radius:5px;">View Course</a>
                    <hr style="margin:20px 0;"/>
                    <p style="font-size:12px;color:#888;">© 2025 %s • %s • <a href="%s">%s</a></p>
                  </div>
                </div>
                """.formatted(courseTitle, description != null ? description : "A new learning opportunity awaits!", companyName, companyName, ownerName, website, website);

        for (User u : students) {
            try {
                sendBrevoEmail(u.getEmail(), "📚 New Course: " + courseTitle, html);
            } catch (Exception ex) {
                log.error("Failed to send course email to {}: {}", u.getEmail(), ex.getMessage());
            }
        }
    }

    // ============================================================
    // 🔹 3. Send New Lesson / Chapter / Video Email
    // ============================================================
    public void sendNewLessonEmail(String courseTitle, String lessonTitle, List<User> students) {
        if (students == null || students.isEmpty()) return;

        String html = """
                <div style="font-family:Arial,sans-serif;background:#f6f9fc;padding:25px;">
                  <div style="max-width:600px;margin:auto;background:white;border-radius:10px;padding:25px;">
                    <h2 style="color:#16a34a;">🎥 New Lesson Added</h2>
                    <p>A new lesson <b>%s</b> has been added to your course <b>%s</b>.</p>
                    <p>Log in to your dashboard and continue learning!</p>
                    <a href="%s" style="background:#16a34a;color:white;text-decoration:none;padding:10px 20px;border-radius:6px;">Go to Course</a>
                    <hr style="margin:20px 0;"/>
                    <p style="font-size:12px;color:#888;">© 2025 %s • %s • <a href="%s">%s</a></p>
                  </div>
                </div>
                """.formatted(lessonTitle, courseTitle, website, companyName, ownerName, website, website);

        for (User u : students) {
            try {
                sendBrevoEmail(u.getEmail(), "🎥 New Lesson in " + courseTitle, html);
            } catch (Exception ex) {
                log.error("Failed to send lesson email to {}: {}", u.getEmail(), ex.getMessage());
            }
        }
    }

    // ============================================================
    // 🔹 Helper: Send Email via Brevo REST API
    // ============================================================
    private void sendBrevoEmail(String toEmail, String subject, String htmlContent) {
        if (toEmail == null || brevoApiKey == null || brevoApiKey.isBlank()) {
            log.warn("Brevo API key missing or invalid email address, skipping send.");
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("sender", Map.of("email", mailFrom, "name", ownerName));
        payload.put("to", List.of(Map.of("email", toEmail)));
        payload.put("subject", subject);
        payload.put("htmlContent", htmlContent);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", brevoApiKey);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    BREVO_URL,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ Email sent to {} - {}", toEmail, subject);
            } else {
                log.error("❌ Brevo API responded {} for {}", response.getStatusCode(), toEmail);
            }
        } catch (Exception ex) {
            log.error("❌ Failed to send email to {}: {}", toEmail, ex.getMessage());
        }
    }

    // ============================================================
// 🔹 4. Send New Exam Notification
// ============================================================
    public void sendNewExamNotification(Exam exam, List<User> students) {
        if (students == null || students.isEmpty()) return;

        String html = """
            <div style="font-family:Arial,sans-serif;background:#f9fafb;padding:20px;">
              <div style="max-width:600px;margin:auto;background:white;border-radius:10px;padding:25px;box-shadow:0 1px 5px rgba(0,0,0,0.1)">
                <h2 style="color:#9333ea;">🧠 New Exam Scheduled: %s</h2>
                <p><b>Type:</b> %s</p>
                <p><b>Language:</b> %s</p>
                <p><b>Start:</b> %s</p>
                <p><b>End:</b> %s</p>
                <p><b>Duration:</b> %s minutes</p>
                <br/>
                <p>Prepare yourself and give your best performance 💪</p>
                <a href="%s" style="background:#9333ea;color:white;padding:10px 18px;text-decoration:none;border-radius:5px;">View Exam Details</a>
                <hr style="margin:20px 0;"/>
                <p style="font-size:12px;color:#888;">© 2025 %s • %s • <a href="%s">%s</a></p>
              </div>
            </div>
            """.formatted(
                exam.getName(),
                exam.getType(),
                exam.getLanguage(),
                exam.getStartDate(),
                exam.getEndDate(),
                exam.getDuration(),
                website,
                companyName, ownerName, website, website
        );

        for (User u : students) {
            try {
                sendBrevoEmail(u.getEmail(), "🧠 New Exam: " + exam.getName(), html);
            } catch (Exception ex) {
                log.error("Failed to send exam email to {}: {}", u.getEmail(), ex.getMessage());
            }
        }
    }

}
