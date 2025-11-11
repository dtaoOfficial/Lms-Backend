package com.dtao.lms.service;

import com.dtao.lms.model.Course;
import com.dtao.lms.model.Enrollment;
import com.dtao.lms.model.EnrollmentAudit;
import com.dtao.lms.repo.CourseRepository;
import com.dtao.lms.repo.EnrollmentAuditRepository;
import com.dtao.lms.repo.EnrollmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepo;
    private final CourseRepository courseRepo;
    private final EnrollmentAuditRepository auditRepo;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${brevo.api.key}")
    private String brevoApiKey;

    @Value("${app.admin.notify:}")
    private String adminNotify;

    @Value("${app.mail.from:}")
    private String mailFrom;

    @Value("${APP_MAIL_OWNER:Admin}")
    private String ownerName;

    @Value("${APP_MAIL_COMPANY:DTAO OFFICIAL}")
    private String companyName;

    @Value("${APP_MAIL_WEBSITE:https://dtaoofficial.netlify.app/}")
    private String website;

    public EnrollmentService(
            EnrollmentRepository enrollmentRepo,
            CourseRepository courseRepo,
            EnrollmentAuditRepository auditRepo
    ) {
        this.enrollmentRepo = enrollmentRepo;
        this.courseRepo = courseRepo;
        this.auditRepo = auditRepo;
    }

    /**
     * Student → create enrollment request
     */
    public Enrollment createEnrollmentRequest(String courseId, String email, String userId) {
        Optional<Course> maybeCourse = courseRepo.findById(courseId);
        if (maybeCourse.isEmpty()) throw new RuntimeException("Course not found");
        Course course = maybeCourse.get();

        Optional<Enrollment> existing = enrollmentRepo.findByCourseIdAndEmail(courseId, email);
        if (existing.isPresent()) {
            Enrollment e = existing.get();
            if ("APPROVED".equalsIgnoreCase(e.getStatus()))
                throw new RuntimeException("Already enrolled in this course");
            if ("PENDING".equalsIgnoreCase(e.getStatus()))
                return e;
        }

        Enrollment e = new Enrollment();
        e.setCourseId(courseId);
        e.setEmail(email);
        e.setUserId(userId);
        e.setStatus("PENDING");
        Instant now = Instant.now();
        e.setCreatedAt(now);
        e.setUpdatedAt(now);
        Enrollment saved = enrollmentRepo.save(e);

        // 1️⃣ Send admin HTML email via Brevo API
        sendAdminHtmlEmail(course, email, userId);

        // 2️⃣ Send student confirmation mail
        sendStudentEnrollmentNotification(email, course);

        // 3️⃣ WebSocket broadcast
        try {
            messagingTemplate.convertAndSend("/topic/enrollments", "NEW_ENROLLMENT");
        } catch (Exception ex) {
            System.err.println("[WS] Failed to broadcast new enrollment: " + ex.getMessage());
        }

        return saved;
    }

    private void sendAdminHtmlEmail(Course course, String studentEmail, String userId) {
        if (adminNotify == null || adminNotify.isBlank()) return;
        try {
            String html = """
                <div style='font-family:Arial,sans-serif;padding:16px;background:#f9fafb'>
                  <div style='max-width:600px;margin:auto;background:white;border-radius:8px;padding:20px'>
                    <h2 style='color:#4f46e5;margin-bottom:8px'>New Enrollment Request</h2>
                    <p>A student has requested enrollment for <b>%s</b>.</p>
                    <table style='margin-top:10px'>
                      <tr><td><b>Course ID:</b></td><td>%s</td></tr>
                      <tr><td><b>Student Email:</b></td><td>%s</td></tr>
                      <tr><td><b>User ID:</b></td><td>%s</td></tr>
                      <tr><td><b>Requested At:</b></td><td>%s</td></tr>
                    </table>
                    <p style='margin-top:20px;'>Review requests in the admin panel.</p>
                    <hr/>
                    <p style='font-size:12px;color:#888'>%s • %s • <a href='%s'>%s</a></p>
                  </div>
                </div>
                """.formatted(
                    course.getTitle(),
                    course.getId(),
                    studentEmail,
                    (userId == null ? "N/A" : userId),
                    Instant.now(),
                    companyName, ownerName, website, website
            );

            sendEmailWithRetry(adminNotify, "📥 New Enrollment Request – " + course.getTitle(), html);

        } catch (Exception ex) {
            System.err.println("[Mail] Failed to send admin HTML email: " + ex.getMessage());
        }
    }

    private void sendStudentEnrollmentNotification(String studentEmail, Course course) {
        try {
            String html = """
                <div style='font-family:Arial,sans-serif;padding:16px;background:#f9fafb'>
                  <div style='max-width:600px;margin:auto;background:white;border-radius:8px;padding:20px'>
                    <h2 style='color:#4f46e5;margin-bottom:8px'>Enrollment Request Submitted ✅</h2>
                    <p>Hi there! You’ve successfully requested enrollment for <b>%s</b>.</p>
                    <p>Our team will review your request shortly. You’ll receive another email once it’s approved.</p>
                    <p style='margin-top:20px;'>Thank you for learning with %s!</p>
                    <hr/>
                    <p style='font-size:12px;color:#888'>%s • %s • <a href='%s'>%s</a></p>
                  </div>
                </div>
                """.formatted(
                    course.getTitle(),
                    companyName,
                    companyName, ownerName, website, website
            );

            sendEmailWithRetry(studentEmail, "📘 Enrollment Request Received – " + course.getTitle(), html);

        } catch (Exception ex) {
            System.err.println("[Mail] Failed to send student enrollment notification: " + ex.getMessage());
        }
    }

    private void sendStudentHtmlEmail(Enrollment e, boolean approved) {
        try {
            Course c = courseRepo.findById(e.getCourseId()).orElse(null);
            String title = (c != null) ? c.getTitle() : "Your course";

            String body = approved
                    ? """
                        <div style='font-family:Arial,sans-serif;padding:16px;background:#f9fafb'>
                          <div style='max-width:600px;margin:auto;background:white;border-radius:8px;padding:20px'>
                            <h2 style='color:#16a34a;margin-bottom:8px'>Enrollment Approved</h2>
                            <p>Your request to join <b>%s</b> has been approved!</p>
                            <p>You can now access all course materials in your student dashboard.</p>
                            %s
                            <hr/>
                            <p style='font-size:12px;color:#888'>%s • %s • <a href='%s'>%s</a></p>
                          </div>
                        </div>
                        """.formatted(title,
                    e.getNotes() != null ? "<p><b>Note:</b> " + e.getNotes() + "</p>" : "",
                    companyName, ownerName, website, website)
                    : """
                        <div style='font-family:Arial,sans-serif;padding:16px;background:#f9fafb'>
                          <div style='max-width:600px;margin:auto;background:white;border-radius:8px;padding:20px'>
                            <h2 style='color:#dc2626;margin-bottom:8px'>Enrollment Not Approved</h2>
                            <p>Your enrollment request for <b>%s</b> was not approved.</p>
                            %s
                            <p>If you think this was an error, please contact support.</p>
                            <hr/>
                            <p style='font-size:12px;color:#888'>%s • %s • <a href='%s'>%s</a></p>
                          </div>
                        </div>
                        """.formatted(title,
                    e.getNotes() != null ? "<p><b>Reason:</b> " + e.getNotes() + "</p>" : "",
                    companyName, ownerName, website, website);

            sendEmailWithRetry(e.getEmail(), approved ? "✅ Enrollment Approved – " + title : "❌ Enrollment Update – " + title, body);

        } catch (Exception ex) {
            System.err.println("[Mail] Failed to send student HTML email: " + ex.getMessage());
        }
    }

    /**
     * ✅ Retry-enabled Brevo mail sender (2 retries, 2s delay)
     */
    private void sendEmailWithRetry(String to, String subject, String htmlContent) {
        int maxRetries = 2;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                sendEmailViaBrevo(to, subject, htmlContent);
                return; // success
            } catch (Exception ex) {
                System.err.println("⚠️ Attempt " + attempt + " failed for " + to + ": " + ex.getMessage());
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ignored) {}
                } else {
                    System.err.println("❌ Giving up after " + attempt + " attempts for " + to);
                }
            }
        }
    }

    private void sendEmailViaBrevo(String to, String subject, String htmlContent) {
        String url = "https://api.brevo.com/v3/smtp/email";

        Map<String, Object> body = new HashMap<>();
        body.put("sender", Map.of("email", mailFrom, "name", companyName));
        body.put("to", List.of(Map.of("email", to)));
        body.put("subject", subject);
        body.put("htmlContent", htmlContent);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", brevoApiKey);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Brevo API failed: " + response.getBody());
        }

        System.out.println("📧 Email sent to " + to);
    }

    // ===== Remaining business logic unchanged ===== //

    public Optional<Enrollment> getEnrollmentForUser(String courseId, String email) {
        return enrollmentRepo.findByCourseIdAndEmail(courseId, email);
    }

    public List<Enrollment> getEnrollmentsByEmail(String email) {
        return enrollmentRepo.findByEmail(email);
    }

    public List<Enrollment> getEnrollmentsByStatus(String status) {
        return status == null
                ? enrollmentRepo.findByStatus("PENDING")
                : enrollmentRepo.findByStatus(status.toUpperCase());
    }

    public List<Enrollment> getEnrollmentsForCourse(String courseId) {
        return enrollmentRepo.findByCourseId(courseId);
    }

    public Enrollment approveEnrollment(String id, String approverNote, String adminEmail) {
        Enrollment e = enrollmentRepo.findById(id).orElseThrow(() -> new RuntimeException("Enrollment not found"));
        e.setStatus("APPROVED");
        e.setNotes(approverNote);
        e.setUpdatedAt(Instant.now());
        Enrollment saved = enrollmentRepo.save(e);

        auditRepo.save(new EnrollmentAudit(
                e.getId(), e.getCourseId(), e.getEmail(),
                "APPROVED", approverNote, adminEmail, Instant.now()
        ));

        sendStudentHtmlEmail(e, true);
        return saved;
    }

    public Enrollment rejectEnrollment(String id, String note, String adminEmail) {
        Enrollment e = enrollmentRepo.findById(id).orElseThrow(() -> new RuntimeException("Enrollment not found"));
        e.setStatus("REJECTED");
        e.setNotes(note);
        e.setUpdatedAt(Instant.now());
        Enrollment saved = enrollmentRepo.save(e);

        auditRepo.save(new EnrollmentAudit(
                e.getId(), e.getCourseId(), e.getEmail(),
                "REJECTED", note, adminEmail, Instant.now()
        ));

        sendStudentHtmlEmail(e, false);
        return saved;
    }

    public void deleteEnrollment(String id) {
        enrollmentRepo.deleteById(id);
    }

    public long countEnrollmentsByEmail(String email) {
        if (email == null || email.isBlank()) return 0;
        try {
            return enrollmentRepo.countByEmailRegex(email);
        } catch (Exception e) {
            System.err.println("[EnrollmentService] Failed to count enrollments: " + e.getMessage());
            return 0;
        }
    }
}
