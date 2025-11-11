package com.dtao.lms.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "comments")
public class Comment {
    @Id
    private String id;

    private TargetType targetType;
    @Indexed
    private String targetId;

    private String email;
    private String text;
    private Instant createdAt;
    private Instant editedAt;
    private boolean deleted = false;

    public Comment() {}

    public Comment(TargetType targetType, String targetId, String email, String text) {
        this.targetType = targetType;
        this.targetId = targetId;
        this.email = email;
        this.text = text;
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
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getEditedAt() { return editedAt; }
    public void setEditedAt(Instant editedAt) { this.editedAt = editedAt; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
}
