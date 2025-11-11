package com.dtao.lms.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "reports")
public class Report {
    @Id
    private String id;

    private TargetType targetType;
    @Indexed
    private String targetId;

    private String email; // reporter
    private String reason; // enum-ish (string)
    private String text; // optional details
    private String status; // OPEN, REVIEWED, CLOSED
    private String handledBy; // admin email
    private Instant createdAt;
    private Instant handledAt;
    private String adminNote;

    public Report() {}

    public Report(TargetType targetType, String targetId, String email, String reason, String text) {
        this.targetType = targetType;
        this.targetId = targetId;
        this.email = email;
        this.reason = reason;
        this.text = text;
        this.status = "OPEN";
        this.createdAt = Instant.now();
    }

    // getters & setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public TargetType getTargetType() { return targetType; }
    public void setTargetType(TargetType targetType) { this.targetType = targetType; }
    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getHandledBy() { return handledBy; }
    public void setHandledBy(String handledBy) { this.handledBy = handledBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getHandledAt() { return handledAt; }
    public void setHandledAt(Instant handledAt) { this.handledAt = handledAt; }
    public String getAdminNote() { return adminNote; }
    public void setAdminNote(String adminNote) { this.adminNote = adminNote; }
}
