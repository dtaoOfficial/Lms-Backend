package com.dtao.lms.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "leaderboard_audit")
public class LeaderboardAudit {

    @Id
    private String id;

    private String adminEmail;
    private String scope; // GLOBAL or COURSE_ID
    private String note;
    private Instant resetAt;

    public LeaderboardAudit() {}

    public LeaderboardAudit(String adminEmail, String scope, String note) {
        this.adminEmail = adminEmail;
        this.scope = scope;
        this.note = note;
        this.resetAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAdminEmail() {
        return adminEmail;
    }

    public void setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Instant getResetAt() {
        return resetAt;
    }

    public void setResetAt(Instant resetAt) {
        this.resetAt = resetAt;
    }
}
