package com.dtao.lms.model;

/**
 * 🏆 LeaderboardEntry
 * Represents a single student's leaderboard record — combining
 * progress, likes, XP, level, and badge.
 */
public class LeaderboardEntry {

    private String email;
    private String name;
    private double progressPercent;
    private long totalLikes;
    private int rank;
    private int xp;
    private int level;
    private String badge;

    // 🔹 No-arg constructor
    public LeaderboardEntry() {}

    // 🔹 All-args constructor (for code using new LeaderboardEntry(...))
    public LeaderboardEntry(String email, String name, double progressPercent, long totalLikes, int rank) {
        this.email = email;
        this.name = name;
        this.progressPercent = progressPercent;
        this.totalLikes = totalLikes;
        this.rank = rank;
    }

    // 🔹 Full constructor (for future compatibility)
    public LeaderboardEntry(String email, String name, double progressPercent, long totalLikes, int rank, int xp, int level, String badge) {
        this.email = email;
        this.name = name;
        this.progressPercent = progressPercent;
        this.totalLikes = totalLikes;
        this.rank = rank;
        this.xp = xp;
        this.level = level;
        this.badge = badge;
    }

    // --- Getters and Setters ---
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getProgressPercent() { return progressPercent; }
    public void setProgressPercent(double progressPercent) { this.progressPercent = progressPercent; }

    public long getTotalLikes() { return totalLikes; }
    public void setTotalLikes(long totalLikes) { this.totalLikes = totalLikes; }

    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }

    public int getXp() { return xp; }
    public void setXp(int xp) { this.xp = xp; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public String getBadge() { return badge; }
    public void setBadge(String badge) { this.badge = badge; }
}
