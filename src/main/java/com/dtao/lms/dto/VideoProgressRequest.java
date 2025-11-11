package com.dtao.lms.dto;

public class VideoProgressRequest {
    private Double lastPosition;
    private Double duration;
    private Boolean completed;

    public VideoProgressRequest() {}

    public Double getLastPosition() { return lastPosition; }
    public void setLastPosition(Double lastPosition) { this.lastPosition = lastPosition; }

    public Double getDuration() { return duration; }
    public void setDuration(Double duration) { this.duration = duration; }

    public Boolean getCompleted() { return completed; }
    public void setCompleted(Boolean completed) { this.completed = completed; }
}
