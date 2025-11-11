package com.dtao.lms.dto;

import com.dtao.lms.model.Notification;

import java.time.Instant;

public class NotificationResponse {
    private String id;
    private String title;
    private String message;
    private String type;
    private boolean read;
    private Instant createdAt;

    public NotificationResponse(Notification n) {
        this.id = n.getId();
        this.title = n.getTitle();
        this.message = n.getMessage();
        this.type = n.getType();
        this.read = n.isRead();
        this.createdAt = n.getCreatedAt();
    }

    // Getters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getType() { return type; }
    public boolean isRead() { return read; }
    public Instant getCreatedAt() { return createdAt; }
}
