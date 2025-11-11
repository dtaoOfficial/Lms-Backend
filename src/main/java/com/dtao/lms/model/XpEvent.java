package com.dtao.lms.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "xp_events")
public class XpEvent {

    @Id
    private String id;

    private String email;         // student email
    private String type;          // VIDEO, DISCUSSION, COURSE
    private int score;            // XP amount
    private String videoId;       // optional
    private String questionId;    // optional
    private String courseId;      // optional
    private String message;       // e.g. "Completed video: Intro to Java"
    private Instant createdAt;    // when XP earned

    public XpEvent() {}

    public XpEvent(String email, String type, int score,
                   String videoId, String questionId, String courseId, String message) {
        this.email = email;
        this.type = type;
        this.score = score;
        this.videoId = videoId;
        this.questionId = questionId;
        this.courseId = courseId;
        this.message = message;
        this.createdAt = Instant.now();
    }

    // ✅ Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public String getVideoId() { return videoId; }
    public void setVideoId(String videoId) { this.videoId = videoId; }

    public String getQuestionId() { return questionId; }
    public void setQuestionId(String questionId) { this.questionId = questionId; }

    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
