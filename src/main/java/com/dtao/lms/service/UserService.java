package com.dtao.lms.service;

import com.dtao.lms.model.EmailVerificationToken;
import com.dtao.lms.model.User;
import com.dtao.lms.payload.UpdateProfileRequest;
import com.dtao.lms.repo.EmailVerificationRepository;
import com.dtao.lms.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class UserService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^[6-9][0-9]{9}$");

    private static final Set<String> ALLOWED_EMAIL_DOMAINS = Set.of(
            "gmail.com", "google.com", "outlook.com", "hotmail.com", "live.com", "microsoft.com"
    );

    private static final int VERIFICATION_TOKEN_MINUTES = 10;
    private static final int RESEND_COOLDOWN_SECONDS = 60;
    private static final int RESEND_MAX_PER_HOUR = 5;

    @Autowired private UserRepository userRepository;
    @Autowired private EmailVerificationRepository emailVerificationRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Autowired private EmailNotificationService emailNotificationService;

    @Value("${app.mail.from}")
    private String mailFrom;

    private final RestTemplate restTemplate = new RestTemplate();

    // ✅ FIXED: use uppercase env variable to match Brevo API key
    @Value("${BREVO_API_KEY:}")
    private String brevoApiKey;

    @Value("${APP_MAIL_COMPANY:DTAO OFFICIAL}")
    private String companyName;

    @Value("${APP_MAIL_OWNER:Admin}")
    private String ownerName;

    @Value("${APP_MAIL_WEBSITE:https://dtaoofficial.netlify.app/}")
    private String website;

    // ==============================
    // Registration + OTP
    // ==============================
    public User registerUser(User user) {
        if (user == null) throw new RuntimeException("User payload is required");
        if (user.getName() == null || user.getName().trim().isEmpty())
            throw new RuntimeException("Name is required");

        String role = user.getRole();
        if (role == null || role.isBlank()) user.setRole(User.Roles.STUDENT);
        else user.setRole(role.trim().toUpperCase());

        String email = Optional.ofNullable(user.getEmail())
                .map(e -> e.trim().toLowerCase())
                .orElseThrow(() -> new RuntimeException("Email is required"));
        if (!email.contains("@")) throw new RuntimeException("Invalid email format");

        String domain = email.substring(email.lastIndexOf("@") + 1);
        if (!ALLOWED_EMAIL_DOMAINS.contains(domain))
            throw new RuntimeException("Email domain not allowed");

        String phone = user.getPhone();
        if (phone != null && !phone.isBlank()) {
            if (!PHONE_PATTERN.matcher(phone).matches())
                throw new RuntimeException("Invalid phone number");
            if (userRepository.findByPhone(phone).isPresent())
                throw new RuntimeException("Phone already registered");
        }

        if (userRepository.findByEmail(email).isPresent())
            throw new RuntimeException("Email already registered");

        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        user.setEmail(email);
        user.setCreatedAt(Instant.now());
        user.setVerified(false);
        user.setActive(true);

        User saved = userRepository.save(user);

        // ✅ Generate OTP and save
        String otp = generateSixDigitOtp();
        EmailVerificationToken token = new EmailVerificationToken();
        token.setEmail(saved.getEmail());
        token.setOtp(otp);
        token.setCreatedAt(Instant.now());
        token.setExpiresAt(Instant.now().plus(VERIFICATION_TOKEN_MINUTES, ChronoUnit.MINUTES));
        token.setLastSentAt(Instant.now());
        token.setSendCountLastHour(1);
        emailVerificationRepository.save(token);

        // ✅ Send OTP Email
        try {
            sendVerificationOtpEmail(saved.getEmail(), saved.getName(), otp);
        } catch (Exception e) {
            System.err.println("[UserService] Failed to send OTP: " + e.getMessage());
        }

        // ❌ Removed welcome email — now sent only after OTP verification
        return saved;
    }

    private String generateSixDigitOtp() {
        return String.valueOf(100000 + new Random().nextInt(900000));
    }

    // ✅ Send OTP via Brevo REST API
    private void sendVerificationOtpEmail(String to, String name, String otp) {
        String subject = "Your LMS Verification Code";
        String html = """
            <div style='font-family:Arial,sans-serif;padding:16px;background:#f9fafb'>
              <div style='max-width:600px;margin:auto;background:white;border-radius:8px;padding:20px'>
                <h2 style='color:#4f46e5;margin-bottom:8px'>Email Verification Code</h2>
                <p>Hello %s,</p>
                <p>Your verification code is: <b style='font-size:18px;'>%s</b></p>
                <p>This code will expire in %d minutes.</p>
                <p>If you didn’t request this, you can ignore this email.</p>
                <hr/>
                <p style='font-size:12px;color:#888'>%s • %s • <a href='%s'>%s</a></p>
              </div>
            </div>
            """.formatted(
                (name == null || name.isBlank()) ? "User" : name,
                otp,
                VERIFICATION_TOKEN_MINUTES,
                companyName, ownerName, website, website
        );

        sendEmailWithRetry(to, subject, html);
    }

    // ==============================
    // OTP Resend + Verification
    // ==============================
    public void resendOtp(String email) {
        if (email == null) throw new RuntimeException("Email required");
        String norm = email.trim().toLowerCase();

        var maybeUser = userRepository.findByEmail(norm);
        if (maybeUser.isEmpty()) throw new RuntimeException("User not found");

        var tokenOpt = emailVerificationRepository.findByEmail(norm);
        Instant now = Instant.now();

        EmailVerificationToken token = tokenOpt.orElseGet(() -> {
            EmailVerificationToken t = new EmailVerificationToken();
            t.setEmail(norm);
            return t;
        });

        if (token.getLastSentAt() != null) {
            long secondsSinceLast = ChronoUnit.SECONDS.between(token.getLastSentAt(), now);
            if (secondsSinceLast < RESEND_COOLDOWN_SECONDS)
                throw new RuntimeException("Please wait before requesting another OTP");
        }

        if (token.getSendCountLastHour() >= RESEND_MAX_PER_HOUR)
            throw new RuntimeException("Too many OTP requests. Try later.");

        String newOtp = generateSixDigitOtp();
        token.setOtp(newOtp);
        token.setCreatedAt(now);
        token.setExpiresAt(now.plus(VERIFICATION_TOKEN_MINUTES, ChronoUnit.MINUTES));
        token.setLastSentAt(now);
        token.setSendCountLastHour(token.getSendCountLastHour() + 1);
        emailVerificationRepository.save(token);

        sendVerificationOtpEmail(norm, maybeUser.get().getName(), newOtp);
    }

    // ✅ FIXED: Welcome email after verification only
    public void verifyEmail(String email, String otp) {
        if (email == null || otp == null)
            throw new RuntimeException("Email and OTP required");

        var tokenOpt = emailVerificationRepository.findByEmailAndOtp(email, otp);
        if (tokenOpt.isEmpty())
            throw new RuntimeException("Invalid or expired OTP");

        EmailVerificationToken token = tokenOpt.get();
        if (token.getExpiresAt().isBefore(Instant.now())) {
            emailVerificationRepository.deleteByEmail(email);
            throw new RuntimeException("OTP expired");
        }

        var userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) throw new RuntimeException("User not found");

        User user = userOpt.get();
        user.setVerified(true);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        emailVerificationRepository.deleteByEmail(email);

        // ✅ Send welcome email AFTER verification
        try {
            if (emailNotificationService != null) {
                emailNotificationService.sendWelcomeEmail(user);
            }
        } catch (Exception ex) {
            System.err.println("[UserService] Failed to send post-verification welcome email: " + ex.getMessage());
        }
    }

    // ==============================
    // Common User Operations
    // ==============================
    public Optional<User> getUserByEmail(String email) {
        if (email == null) return Optional.empty();
        return userRepository.findByEmail(email.trim().toLowerCase());
    }

    public Optional<User> getUserById(String id) {
        return userRepository.findById(id);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public void deleteUser(String id) {
        userRepository.deleteById(id);
    }

    public Optional<User> updateUser(String id, User newData) {
        if (id == null || id.isBlank()) throw new RuntimeException("User ID is required");

        return userRepository.findById(id).map(existing -> {
            if (newData.getName() != null && !newData.getName().isBlank())
                existing.setName(newData.getName().trim());
            if (newData.getEmail() != null && !newData.getEmail().isBlank())
                existing.setEmail(newData.getEmail().trim().toLowerCase());
            if (newData.getPhone() != null && !newData.getPhone().isBlank())
                existing.setPhone(newData.getPhone().trim());
            if (newData.getRole() != null && !newData.getRole().isBlank())
                existing.setRole(newData.getRole().trim().toUpperCase());
            if (newData.getDepartment() != null && !newData.getDepartment().isBlank())
                existing.setDepartment(newData.getDepartment().trim());
            if (newData.getPasswordHash() != null && !newData.getPasswordHash().isBlank())
                existing.setPasswordHash(passwordEncoder.encode(newData.getPasswordHash().trim()));

            existing.setUpdatedAt(Instant.now());
            return userRepository.save(existing);
        });
    }

    public User updateStudentProfile(String email, UpdateProfileRequest req) {
        if (email == null) throw new RuntimeException("Email required");

        var userOpt = userRepository.findByEmail(email.trim().toLowerCase());
        if (userOpt.isEmpty()) throw new RuntimeException("User not found");

        User user = userOpt.get();

        if (req.getName() != null && !req.getName().isBlank())
            user.setName(req.getName().trim());

        if (req.getPhone() != null && !req.getPhone().isBlank()) {
            String phone = req.getPhone().trim();
            if (!PHONE_PATTERN.matcher(phone).matches())
                throw new RuntimeException("Invalid phone number");

            userRepository.findByPhone(phone).ifPresent(existing -> {
                if (!existing.getId().equals(user.getId()))
                    throw new RuntimeException("Phone number already in use");
            });
            user.setPhone(phone);
        }

        if (req.getDepartment() != null && !req.getDepartment().isBlank())
            user.setDepartment(req.getDepartment().trim());

        if (req.getAbout() != null)
            user.setAbout(req.getAbout().trim());

        user.setUpdatedAt(Instant.now());
        return userRepository.save(user);
    }

    public void changePassword(String email, String oldPassword, String newPassword) {
        if (email == null) throw new RuntimeException("Email required");
        if (oldPassword == null || oldPassword.isBlank())
            throw new RuntimeException("Old password required");
        if (newPassword == null || newPassword.length() < 8)
            throw new RuntimeException("New password must be at least 8 characters");

        var maybe = userRepository.findByEmail(email.trim().toLowerCase());
        if (maybe.isEmpty()) throw new RuntimeException("User not found");

        User user = maybe.get();
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash()))
            throw new RuntimeException("Old password incorrect");

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
    }

    // ==============================
    // Login Lockout Helpers
    // ==============================
    public void recordFailedLogin(String email, int maxAttempts, long lockMinutes) {
        userRepository.findByEmail(email).ifPresent(u -> {
            u.setFailedLoginAttempts(u.getFailedLoginAttempts() + 1);
            if (u.getFailedLoginAttempts() >= maxAttempts)
                u.setLockedUntil(Instant.now().plus(lockMinutes, ChronoUnit.MINUTES));
            userRepository.save(u);
        });
    }

    public void resetFailedLogin(String email) {
        userRepository.findByEmail(email).ifPresent(u -> {
            u.setFailedLoginAttempts(0);
            u.setLockedUntil(null);
            userRepository.save(u);
        });
    }

    // ==============================
    // Brevo helper methods
    // ==============================
    private void sendEmailWithRetry(String to, String subject, String htmlContent) {
        int maxRetries = 2;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                sendEmailViaBrevo(to, subject, htmlContent);
                return;
            } catch (Exception ex) {
                System.err.println("⚠️ Attempt " + attempt + " failed for " + to + ": " + ex.getMessage());
                if (attempt < maxRetries) {
                    try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                } else {
                    System.err.println("❌ Giving up after " + attempt + " attempts for " + to);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void sendEmailViaBrevo(String to, String subject, String htmlContent) {
        if (brevoApiKey == null || brevoApiKey.isBlank()) {
            throw new RuntimeException("Brevo API key not configured (BREVO_API_KEY)");
        }

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
            throw new RuntimeException("Brevo API failed: " + response.getStatusCode() + " " + response.getBody());
        }

        System.out.println("📧 Email sent to " + to + " via Brevo");
    }
}
