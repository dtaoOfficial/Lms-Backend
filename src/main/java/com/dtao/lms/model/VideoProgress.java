package com.dtao.lms.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * VideoProgress
 *
 * Represents a user's playback progress for a specific video.
 * Each user+video pair is unique (enforced by a compound index).
 */
@Document(collection = "video_progress")
@CompoundIndex(name = "email_video_idx", def = "{'email': 1, 'videoId': 1}", unique = true)
public class VideoProgress {

    @Id
    private String id;

    private String email;
    private String videoId;
    private Double lastPosition = 0.0;   // seconds (safe default)
    private Double duration = 0.0;       // total video duration (optional)
    private boolean completed = false;
    private Instant updatedAt;
    private String videoTitle; // ✅ Add this if missing


    public VideoProgress() {}

    public VideoProgress(String email, String videoId, Double lastPosition, Double duration, boolean completed) {
        this.email = email;
        this.videoId = videoId;
        this.lastPosition = sanitize(lastPosition);
        this.duration = sanitize(duration);
        this.completed = completed;
        this.updatedAt = Instant.now();
    }

    /** Ensure no null or negative numbers sneak in. */
    private static double sanitize(Double value) {
        if (value == null || value.isNaN() || value.isInfinite()) return 0.0;
        return Math.max(0.0, value);
    }

    // --- Getters & Setters ---
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public Double getLastPosition() {
        return lastPosition == null ? 0.0 : lastPosition;
    }

    public void setLastPosition(Double lastPosition) {
        this.lastPosition = sanitize(lastPosition);
    }

    public Double getDuration() {
        return duration == null ? 0.0 : duration;
    }

    public void setDuration(Double duration) {
        this.duration = sanitize(duration);
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getVideoTitle() {
        return videoTitle;
    }

    public void setVideoTitle(String videoTitle) {
        this.videoTitle = videoTitle;
    }

    // --- convenience helpers ---
    public void touch() {
        this.updatedAt = Instant.now();
    }

    public void markCompleted() {
        this.completed = true;
        this.updatedAt = Instant.now();
    }

    @Override
    public String toString() {
        return "VideoProgress{" +
                "email='" + email + '\'' +
                ", videoId='" + videoId + '\'' +
                ", lastPosition=" + lastPosition +
                ", duration=" + duration +
                ", completed=" + completed +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
