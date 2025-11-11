package com.dtao.lms.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "likes")
@CompoundIndex(name = "unique_target_email", def = "{'targetType': 1, 'targetId': 1, 'email': 1}", unique = true)
public class LikeRecord {
    @Id
    private String id;

    private TargetType targetType;
    private String targetId;
    private String email;
    private LikeType type;
    private Instant createdAt;

    public LikeRecord() {}

    public LikeRecord(TargetType targetType, String targetId, String email, LikeType type) {
        this.targetType = targetType;
        this.targetId = targetId;
        this.email = email;
        this.type = type;
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
    public LikeType getType() { return type; }
    public void setType(LikeType type) { this.type = type; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
