package com.dtao.lms.dto;

public class LeaderboardResetRequest {
    private String scope; // GLOBAL or COURSE_ID
    private String note;  // optional admin note

    public LeaderboardResetRequest() {}

    public LeaderboardResetRequest(String scope, String note) {
        this.scope = scope;
        this.note = note;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
