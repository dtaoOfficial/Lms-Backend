package com.dtao.lms.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "email_verifications")
public class EmailVerificationToken {

    @Id
    private String id;

    private String email;
    private String otp; // 6-digit OTP

    // Keep expiresAt as the date field that TTL index will use.
    private Instant expiresAt;

    private Instant createdAt;
    private Instant lastSentAt;
    private int sendCountLastHour = 0;

    public EmailVerificationToken() {}

    // getters / setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getLastSentAt() { return lastSentAt; }
    public void setLastSentAt(Instant lastSentAt) { this.lastSentAt = lastSentAt; }

    public int getSendCountLastHour() { return sendCountLastHour; }
    public void setSendCountLastHour(int sendCountLastHour) { this.sendCountLastHour = sendCountLastHour; }
}
