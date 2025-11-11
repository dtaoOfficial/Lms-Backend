package com.dtao.lms.dto;

/**
 * 🎮 XPUpdateResponse
 * Supports both old and new naming styles for compatibility.
 */
public class XPUpdateResponse {

    private boolean success;
    private String message;
    private int newXp;
    private int newLevel;
    private String newBadge;

    public XPUpdateResponse() {}

    public XPUpdateResponse(boolean success, String message, int newXp, int newLevel, String newBadge) {
        this.success = success;
        this.message = message;
        this.newXp = newXp;
        this.newLevel = newLevel;
        this.newBadge = newBadge;
    }

    // ✅ Existing Getters/Setters (unchanged)
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public int getNewXp() { return newXp; }
    public void setNewXp(int newXp) { this.newXp = newXp; }

    public int getNewLevel() { return newLevel; }
    public void setNewLevel(int newLevel) { this.newLevel = newLevel; }

    public String getNewBadge() { return newBadge; }
    public void setNewBadge(String newBadge) { this.newBadge = newBadge; }

    // ✅ Compatibility Aliases (for new service usage)
    public int getXp() { return newXp; }
    public int getLevel() { return newLevel; }
    public String getBadge() { return newBadge; }
}
