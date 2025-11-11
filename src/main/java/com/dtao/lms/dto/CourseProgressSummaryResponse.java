package com.dtao.lms.dto;

import java.time.Instant;
import java.util.List;

/**
 * Summary of course progress for a user (used by frontend My Courses / dashboard).
 */
public class CourseProgressSummaryResponse {
    private String courseId;
    private String courseTitle;
    private int totalVideos;
    private int videosCompleted;
    private double percent;
    private List<String> completedVideoIds;
    private String lastWatchedVideoId;
    private Instant lastWatchedAt;

    public CourseProgressSummaryResponse() {}

    public CourseProgressSummaryResponse(String courseId, String courseTitle, int totalVideos, int videosCompleted,
                                         List<String> completedVideoIds, String lastWatchedVideoId, Instant lastWatchedAt) {
        this.courseId = courseId;
        this.courseTitle = courseTitle;
        this.totalVideos = totalVideos;
        this.videosCompleted = videosCompleted;
        this.completedVideoIds = completedVideoIds;
        this.lastWatchedVideoId = lastWatchedVideoId;
        this.lastWatchedAt = lastWatchedAt;
        this.percent = totalVideos == 0 ? 0.0 : (videosCompleted * 100.0 / totalVideos);
    }

    // convenience constructor (without last watched)
    public CourseProgressSummaryResponse(String courseId, String courseTitle, int totalVideos, int videosCompleted, List<String> completedVideoIds) {
        this(courseId, courseTitle, totalVideos, videosCompleted, completedVideoIds, null, null);
    }

    // getters / setters
    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }

    public String getCourseTitle() { return courseTitle; }
    public void setCourseTitle(String courseTitle) { this.courseTitle = courseTitle; }

    public int getTotalVideos() { return totalVideos; }
    public void setTotalVideos(int totalVideos) { this.totalVideos = totalVideos; }

    public int getVideosCompleted() { return videosCompleted; }
    public void setVideosCompleted(int videosCompleted) { this.videosCompleted = videosCompleted; }

    public double getPercent() { return percent; }
    public void setPercent(double percent) { this.percent = percent; }

    public List<String> getCompletedVideoIds() { return completedVideoIds; }
    public void setCompletedVideoIds(List<String> completedVideoIds) { this.completedVideoIds = completedVideoIds; }

    public String getLastWatchedVideoId() { return lastWatchedVideoId; }
    public void setLastWatchedVideoId(String lastWatchedVideoId) { this.lastWatchedVideoId = lastWatchedVideoId; }

    public Instant getLastWatchedAt() { return lastWatchedAt; }
    public void setLastWatchedAt(Instant lastWatchedAt) { this.lastWatchedAt = lastWatchedAt; }
}
