package com.dtao.lms.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "videos")
public class Video {
    @Id
    private String id;

    private String title;
    private String description;

    // canonical field used by controllers/services
    private String videoUrl;

    // legacy alias
    private String sourceUrl;

    private String sourceType;
    private String contentType;

    private String chapterId;
    private String courseId;

    // use 'order' to match repository method names like findByChapterIdOrderByOrderAsc(...)
    private Integer order = 0;

    private Instant createdAt;
    private Instant updatedAt;

    public Video() {}

    // getters / setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
        if (this.videoUrl == null || this.videoUrl.isBlank()) {
            this.videoUrl = sourceUrl;
        }
    }

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getChapterId() { return chapterId; }
    public void setChapterId(String chapterId) { this.chapterId = chapterId; }

    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }

    public Integer getOrder() { return order; }
    public void setOrder(Integer order) { this.order = order; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
