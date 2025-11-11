package com.dtao.lms.dto;

import java.time.Instant;

/**
 * ✅ Sent after student starts an exam
 * Contains essential session info (for timer and tracking)
 */
public class ExamStartResponse {
    private String examId;
    private String examName;
    private int durationMinutes;
    private int totalQuestions;
    private Instant startTime;

    public ExamStartResponse() {}

    public ExamStartResponse(String examId, String examName, int durationMinutes,
                             int totalQuestions, Instant startTime) {
        this.examId = examId;
        this.examName = examName;
        this.durationMinutes = durationMinutes;
        this.totalQuestions = totalQuestions;
        this.startTime = startTime;
    }

    // Getters and Setters
    public String getExamId() { return examId; }
    public void setExamId(String examId) { this.examId = examId; }
    public String getExamName() { return examName; }
    public void setExamName(String examName) { this.examName = examName; }
    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    public int getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }
    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }

    @Override
    public String toString() {
        return "ExamStartResponse{" +
                "examId='" + examId + '\'' +
                ", examName='" + examName + '\'' +
                ", durationMinutes=" + durationMinutes +
                ", totalQuestions=" + totalQuestions +
                ", startTime=" + startTime +
                '}';
    }

    // Manual Builder
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String examId;
        private String examName;
        private int durationMinutes;
        private int totalQuestions;
        private Instant startTime;

        public Builder examId(String examId) { this.examId = examId; return this; }
        public Builder examName(String examName) { this.examName = examName; return this; }
        public Builder durationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; return this; }
        public Builder totalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; return this; }
        public Builder startTime(Instant startTime) { this.startTime = startTime; return this; }

        public ExamStartResponse build() {
            return new ExamStartResponse(examId, examName, durationMinutes, totalQuestions, startTime);
        }
    }
}
