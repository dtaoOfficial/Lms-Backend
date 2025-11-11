package com.dtao.lms.dto;

import com.dtao.lms.model.LeaderboardEntry;

import java.util.List;

/**
 * DTO for sending leaderboard data to frontend.
 * Clean and production-safe version.
 */
public class LeaderboardResponse {
    private String courseId;
    private List<LeaderboardEntry> entries;

    // 🔹 No-arg constructor
    public LeaderboardResponse() {}

    // 🔹 All-args constructor (used in LeaderboardService)
    public LeaderboardResponse(String courseId, List<LeaderboardEntry> entries) {
        this.courseId = courseId;
        this.entries = entries;
    }

    // --- Getters and Setters ---
    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }

    public List<LeaderboardEntry> getEntries() { return entries; }
    public void setEntries(List<LeaderboardEntry> entries) { this.entries = entries; }
}
