package com.dtao.lms.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "exams")
public class Exam {

    @Id
    private String id;

    @Field("name")
    private String name;

    @Field("type") // MCQ, CODING, DEBUG
    private String type;

    @Field("language") // Java, C, Python etc.
    private String language;

    @Field("start_date")
    private LocalDateTime startDate;

    @Field("end_date")
    private LocalDateTime endDate;

    @Field("duration_minutes")
    private int duration; // exam duration in minutes

    @Field("is_published")
    private boolean isPublished = false;

    @Field("questions")
    private List<Question> questions = new ArrayList<>();

    @Field("created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Field("created_by")
    private String createdBy;

    // Constructors
    public Exam() {}

    public Exam(String name, String type, String language, LocalDateTime startDate,
                LocalDateTime endDate, int duration, boolean isPublished, String createdBy) {
        this.name = name;
        this.type = type;
        this.language = language;
        this.startDate = startDate;
        this.endDate = endDate;
        this.duration = duration;
        this.isPublished = isPublished;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() { return id; }

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

    public boolean isPublished() { return isPublished; }
    public void setPublished(boolean published) { isPublished = published; }

    public List<Question> getQuestions() { return questions; }
    public void setQuestions(List<Question> questions) { this.questions = questions; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
