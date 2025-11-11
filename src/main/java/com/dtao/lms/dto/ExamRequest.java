package com.dtao.lms.dto;

import java.time.LocalDateTime;

public class ExamRequest {

    private String name;
    private String type;
    private String language;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private int duration;
    private boolean isPublished;
    private String createdBy;

    public ExamRequest() {}

    public ExamRequest(String name, String type, String language,
                       LocalDateTime startDate, LocalDateTime endDate,
                       int duration, boolean isPublished, String createdBy) {
        this.name = name;
        this.type = type;
        this.language = language;
        this.startDate = startDate;
        this.endDate = endDate;
        this.duration = duration;
        this.isPublished = isPublished;
        this.createdBy = createdBy;
    }

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

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
