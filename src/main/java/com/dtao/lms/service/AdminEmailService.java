package com.dtao.lms.service;

import com.dtao.lms.model.EmailLog;
import com.dtao.lms.model.User;
import com.dtao.lms.repo.EmailLogRepository;
import com.dtao.lms.repo.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminEmailService {

    private static final Logger log = LoggerFactory.getLogger(AdminEmailService.class);

    private final UserRepository userRepository;
    private final EmailLogRepository emailLogRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${brevo.api.key}")
    private String brevoApiKey;

    @Value("${app.mail.from:imsingle8688@gmail.com}")
    private String fromEmail;

    @Value("${app.mail.owner:DTAO Admin}")
    private String fromName;

    public AdminEmailService(UserRepository userRepository, EmailLogRepository emailLogRepository) {
        this.userRepository = userRepository;
        this.emailLogRepository = emailLogRepository;
    }

    public EmailLog sendEmail(String subject, String message, String target, String sentBy, List<String> customRecipients) {
        EmailLog logEntry = new EmailLog();
        logEntry.setSubject(subject);
        logEntry.setMessage(message);
        logEntry.setSentBy(sentBy);
        logEntry.setSentAt(Instant.now());
        logEntry.setSuccess(false);

        List<String> recipients = new ArrayList<>();

        try {
            // 🎯 Select recipients based on target
            switch (target.toUpperCase()) {
                case "ALL" -> recipients = userRepository.findAll().stream()
                        .map(User::getEmail)
                        .collect(Collectors.toList());
                case "STUDENT" -> recipients = userRepository.findByRoleIgnoreCase("STUDENT")
                        .stream().map(User::getEmail)
                        .collect(Collectors.toList());
                case "TEACHER" -> recipients = userRepository.findByRoleIgnoreCase("TEACHER")
                        .stream().map(User::getEmail)
                        .collect(Collectors.toList());
                case "CUSTOM" -> recipients = customRecipients != null ? customRecipients : List.of();
                default -> log.warn("⚠ Unknown target type: {}", target);
            }

            logEntry.setRecipients(recipients);

            if (recipients.isEmpty()) {
                log.warn("⚠ No recipients found for target {}", target);
                logEntry.setErrorMessage("No recipients found for target " + target);
                return emailLogRepository.save(logEntry);
            }

            // ✉️ Send each email via Brevo API
            for (String to : recipients) {
                sendEmailViaBrevo(to, subject, message);
            }

            logEntry.setSuccess(true);
            log.info("✅ Broadcast email successfully sent to {} recipients", recipients.size());

        } catch (Exception e) {
            log.error("💥 Failed to send broadcast email: {}", e.getMessage());
            logEntry.setErrorMessage(e.getMessage());
        }

        return emailLogRepository.save(logEntry);
    }

    private void sendEmailViaBrevo(String to, String subject, String htmlContent) {
        try {
            String url = "https://api.brevo.com/v3/smtp/email";

            Map<String, Object> body = new HashMap<>();
            body.put("sender", Map.of("email", fromEmail, "name", fromName));
            body.put("to", List.of(Map.of("email", to)));
            body.put("subject", subject);
            body.put("htmlContent", "<div style='font-family:sans-serif;'>" + htmlContent + "</div>");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", brevoApiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("📨 Email sent to {}", to);
            } else {
                log.error("❌ Brevo API failed for {}: {}", to, response.getBody());
            }

        } catch (Exception e) {
            log.error("💥 Error sending to {}: {}", to, e.getMessage());
        }
    }

    public List<EmailLog> getAllEmailLogs() {
        return emailLogRepository.findAll();
    }

    public void deleteEmailLog(String id) {
        emailLogRepository.deleteById(id);
    }
}
