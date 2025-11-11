package com.dtao.lms.dto;

import java.time.Instant;

public class VideoProgressResponse {
    private String videoId;
    private Double lastPosition;
    private Double duration;
    private boolean completed;
    private Instant updatedAt;

    public VideoProgressResponse() {}

    public VideoProgressResponse(String videoId, Double lastPosition, Double duration, boolean completed, Instant updatedAt) {
        this.videoId = videoId;
        this.lastPosition = lastPosition;
        this.duration = duration;
        this.completed = completed;
        this.updatedAt = updatedAt;
    }

    // getters & setters
    public String getVideoId() { return videoId; }
    public void setVideoId(String videoId) { this.videoId = videoId; }

    public Double getLastPosition() { return lastPosition; }
    public void setLastPosition(Double lastPosition) { this.lastPosition = lastPosition; }

    public Double getDuration() { return duration; }
    public void setDuration(Double duration) { this.duration = duration; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
