package com.dtao.lms.dto;

import com.dtao.lms.model.Question;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ✅ ExamResponse DTO
 * Used for both Admin (list, create) and Student (available exams)
 */
public class ExamResponse {

    private String id;
    private String name;
    private String type;
    private String language;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private int duration;
    private boolean published;
    private List<Question> questions;
    private LocalDateTime createdAt;
    private String createdBy;
    private String studentStatus;

    public ExamResponse() {}

    public ExamResponse(String id, String name, String type, String language, LocalDateTime startDate,
                        LocalDateTime endDate, int duration, boolean published, List<Question> questions,
                        LocalDateTime createdAt, String createdBy, String studentStatus) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.language = language;
        this.startDate = startDate;
        this.endDate = endDate;
        this.duration = duration;
        this.published = published;
        this.questions = questions;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.studentStatus = studentStatus;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }
    public List<Question> getQuestions() { return questions; }
    public void setQuestions(List<Question> questions) { this.questions = questions; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getStudentStatus() { return studentStatus; }
    public void setStudentStatus(String studentStatus) { this.studentStatus = studentStatus; }

    @Override
    public String toString() {
        return "ExamResponse{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", language='" + language + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", duration=" + duration +
                ", published=" + published +
                ", questions=" + questions +
                ", createdAt=" + createdAt +
                ", createdBy='" + createdBy + '\'' +
                ", studentStatus='" + studentStatus + '\'' +
                '}';
    }

    // Builder
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private String id;
        private String name;
        private String type;
        private String language;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private int duration;
        private boolean published;
        private List<Question> questions;
        private LocalDateTime createdAt;
        private String createdBy;
        private String studentStatus;

        public Builder id(String id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder type(String type) { this.type = type; return this; }
        public Builder language(String language) { this.language = language; return this; }
        public Builder startDate(LocalDateTime startDate) { this.startDate = startDate; return this; }
        public Builder endDate(LocalDateTime endDate) { this.endDate = endDate; return this; }
        public Builder duration(int duration) { this.duration = duration; return this; }
        public Builder published(boolean published) { this.published = published; return this; }
        public Builder questions(List<Question> questions) { this.questions = questions; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder createdBy(String createdBy) { this.createdBy = createdBy; return this; }
        public Builder studentStatus(String studentStatus) { this.studentStatus = studentStatus; return this; }

        public ExamResponse build() {
            return new ExamResponse(id, name, type, language, startDate, endDate, duration,
                    published, questions, createdAt, createdBy, studentStatus);
        }
    }
}
