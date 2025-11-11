package com.dtao.lms.dto;

import com.dtao.lms.model.DiscussionReply;

import java.time.LocalDateTime;
import java.util.List;

public class DiscussionResponse {
    private String id;
    private String courseId;
    private String userEmail;
    private String questionText;
    private LocalDateTime createdAt;
    private List<DiscussionReply> replies;

    public DiscussionResponse(String id, String courseId, String userEmail, String questionText,
                              LocalDateTime createdAt, List<DiscussionReply> replies) {
        this.id = id;
        this.courseId = courseId;
        this.userEmail = userEmail;
        this.questionText = questionText;
        this.createdAt = createdAt;
        this.replies = replies;
    }

    // Getters
    public String getId() { return id; }
    public String getCourseId() { return courseId; }
    public String getUserEmail() { return userEmail; }
    public String getQuestionText() { return questionText; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<DiscussionReply> getReplies() { return replies; }
}
