package com.dtao.lms.dto;

import java.util.List;

/**
 * 🎓 StudentDashboardStatsResponse
 * Aggregates all dashboard data for a student:
 * - Enrollments
 * - Video progress
 * - Certificates
 * - Likes
 * - XP / Level / Badge (Gamification)
 */
public class StudentDashboardStatsResponse {

    private long totalCourses;
    private long completedVideos;
    private double averageProgress;
    private long totalCertificates;
    private long totalLikes;
    private List<String> recentVideos;

    // 🧠 Gamification fields
    private int xp;
    private int level;
    private String badge;

    // ✅ Constructor (full version)
    public StudentDashboardStatsResponse(long totalCourses,
                                         long completedVideos,
                                         double averageProgress,
                                         long totalCertificates,
                                         long totalLikes,
                                         List<String> recentVideos,
                                         int xp,
                                         int level,
                                         String badge) {
        this.totalCourses = totalCourses;
        this.completedVideos = completedVideos;
        this.averageProgress = averageProgress;
        this.totalCertificates = totalCertificates;
        this.totalLikes = totalLikes;
        this.recentVideos = recentVideos;
        this.xp = xp;
        this.level = level;
        this.badge = badge;
    }

    // ✅ Constructor (legacy support - without gamification)
    public StudentDashboardStatsResponse(long totalCourses,
                                         long completedVideos,
                                         double averageProgress,
                                         long totalCertificates,
                                         long totalLikes,
                                         List<String> recentVideos) {
        this(totalCourses, completedVideos, averageProgress, totalCertificates, totalLikes, recentVideos, 0, 1, "New Learner 🐣");
    }

    // ✅ Getters & Setters
    public long getTotalCourses() { return totalCourses; }
    public void setTotalCourses(long totalCourses) { this.totalCourses = totalCourses; }

    public long getCompletedVideos() { return completedVideos; }
    public void setCompletedVideos(long completedVideos) { this.completedVideos = completedVideos; }

    public double getAverageProgress() { return averageProgress; }
    public void setAverageProgress(double averageProgress) { this.averageProgress = averageProgress; }

    public long getTotalCertificates() { return totalCertificates; }
    public void setTotalCertificates(long totalCertificates) { this.totalCertificates = totalCertificates; }

    public long getTotalLikes() { return totalLikes; }
    public void setTotalLikes(long totalLikes) { this.totalLikes = totalLikes; }

    public List<String> getRecentVideos() { return recentVideos; }
    public void setRecentVideos(List<String> recentVideos) { this.recentVideos = recentVideos; }

    public int getXp() { return xp; }
    public void setXp(int xp) { this.xp = xp; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public String getBadge() { return badge; }
    public void setBadge(String badge) { this.badge = badge; }
}
