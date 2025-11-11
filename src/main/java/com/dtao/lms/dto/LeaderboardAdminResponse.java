package com.dtao.lms.dto;

import com.dtao.lms.model.LeaderboardEntry;

import java.util.Date;
import java.util.List;

public class LeaderboardAdminResponse {
    private String scope; // GLOBAL or courseId
    private int totalEntries;
    private List<LeaderboardEntry> entries;
    private Date generatedAt;

    public LeaderboardAdminResponse() {}

    public LeaderboardAdminResponse(String scope, List<LeaderboardEntry> entries) {
        this.scope = scope;
        this.entries = entries;
        this.totalEntries = (entries != null) ? entries.size() : 0;
        this.generatedAt = new Date();
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public int getTotalEntries() {
        return totalEntries;
    }

    public void setTotalEntries(int totalEntries) {
        this.totalEntries = totalEntries;
    }

    public List<LeaderboardEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<LeaderboardEntry> entries) {
        this.entries = entries;
    }

    public Date getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Date generatedAt) {
        this.generatedAt = generatedAt;
    }
}
