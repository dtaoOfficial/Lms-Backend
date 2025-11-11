package com.dtao.lms.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "email_logs")
public class EmailLog {

    @Id
    private String id;

    private String subject;
    private String message;
    private List<String> recipients;
    private String sentBy; // admin email
    private Instant sentAt;
    private boolean success;
    private String errorMessage;

    public EmailLog() {}

    public EmailLog(String subject, String message, List<String> recipients, String sentBy, boolean success, String errorMessage) {
        this.subject = subject;
        this.message = message;
        this.recipients = recipients;
        this.sentBy = sentBy;
        this.success = success;
        this.errorMessage = errorMessage;
        this.sentAt = Instant.now();
    }

    // Getters and Setters
    public String getId() { return id; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<String> getRecipients() { return recipients; }
    public void setRecipients(List<String> recipients) { this.recipients = recipients; }

    public String getSentBy() { return sentBy; }
    public void setSentBy(String sentBy) { this.sentBy = sentBy; }

    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
