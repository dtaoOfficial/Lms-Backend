package com.dtao.lms.model;

import java.time.LocalDateTime;

public class DiscussionReply {
    private String userEmail;
    private String replyText;
    private LocalDateTime createdAt = LocalDateTime.now();

    public DiscussionReply() {}

    public DiscussionReply(String userEmail, String replyText) {
        this.userEmail = userEmail;
        this.replyText = replyText;
    }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getReplyText() { return replyText; }
    public void setReplyText(String replyText) { this.replyText = replyText; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
