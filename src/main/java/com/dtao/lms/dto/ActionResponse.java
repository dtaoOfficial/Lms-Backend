package com.dtao.lms.dto;

public class ActionResponse {
    private long likes;
    private long dislikes;
    private String userState; // LIKED | DISLIKED | NONE

    public ActionResponse() {}

    public ActionResponse(long likes, long dislikes, String userState) {
        this.likes = likes;
        this.dislikes = dislikes;
        this.userState = userState;
    }

    public long getLikes() { return likes; }
    public void setLikes(long likes) { this.likes = likes; }
    public long getDislikes() { return dislikes; }
    public void setDislikes(long dislikes) { this.dislikes = dislikes; }
    public String getUserState() { return userState; }
    public void setUserState(String userState) { this.userState = userState; }
}
