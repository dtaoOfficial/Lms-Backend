package com.dtao.lms.dto;

import java.util.ArrayList;
import java.util.List;

public class CourseProgressResponse {
    private String courseId;
    private int totalVideos;
    private int videosCompleted;
    private double percent; // 0.0 - 100.0

    // NEW: list of completed video IDs (for UI per-video badges)
    private List<String> completedVideoIds = new ArrayList<>();

    public CourseProgressResponse() {}

    public CourseProgressResponse(String courseId, int totalVideos, int videosCompleted) {
        this.courseId = courseId;
        this.totalVideos = totalVideos;
        this.videosCompleted = videosCompleted;
        this.percent = totalVideos == 0 ? 0.0 : (videosCompleted * 100.0 / totalVideos);
    }

    public CourseProgressResponse(String courseId, int totalVideos, int videosCompleted, List<String> completedVideoIds) {
        this.courseId = courseId;
        this.totalVideos = totalVideos;
        this.videosCompleted = videosCompleted;
        this.percent = totalVideos == 0 ? 0.0 : (videosCompleted * 100.0 / totalVideos);
        if (completedVideoIds != null) this.completedVideoIds = completedVideoIds;
    }

    // getters & setters
    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }

    public int getTotalVideos() { return totalVideos; }
    public void setTotalVideos(int totalVideos) { this.totalVideos = totalVideos; }

    public int getVideosCompleted() { return videosCompleted; }
    public void setVideosCompleted(int videosCompleted) { this.videosCompleted = videosCompleted; }

    public double getPercent() { return percent; }
    public void setPercent(double percent) { this.percent = percent; }

    public List<String> getCompletedVideoIds() { return completedVideoIds; }
    public void setCompletedVideoIds(List<String> completedVideoIds) { this.completedVideoIds = completedVideoIds; }
}
