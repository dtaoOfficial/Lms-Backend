package com.dtao.lms.dto;

import com.dtao.lms.model.AnswerRecord;

import java.time.Instant;
import java.util.List;

/**
 * ✅ Final result returned after student submits exam
 * Includes score, accuracy, and detailed answer summary
 */
public class ExamResultResponse {

    private String examId;
    private String examName;
    private int totalQuestions;
    private int correctCount;
    private int wrongCount;
    private double percentage;
    private int score;
    private String status;
    private Instant submittedAt;
    private List<AnswerRecord> answers;
    private String performanceMessage;
    private String grade;
    private String studentName;
    private String studentEmail;

    public ExamResultResponse() {}

    public ExamResultResponse(String examId, String examName, int totalQuestions, int correctCount, int wrongCount,
                              double percentage, int score, String status, Instant submittedAt,
                              List<AnswerRecord> answers, String performanceMessage, String grade,
                              String studentName, String studentEmail) {
        this.examId = examId;
        this.examName = examName;
        this.totalQuestions = totalQuestions;
        this.correctCount = correctCount;
        this.wrongCount = wrongCount;
        this.percentage = percentage;
        this.score = score;
        this.status = status;
        this.submittedAt = submittedAt;
        this.answers = answers;
        this.performanceMessage = performanceMessage;
        this.grade = grade;
        this.studentName = studentName;
        this.studentEmail = studentEmail;
    }

    // Getters and Setters
    public String getExamId() { return examId; }
    public void setExamId(String examId) { this.examId = examId; }
    public String getExamName() { return examName; }
    public void setExamName(String examName) { this.examName = examName; }
    public int getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }
    public int getCorrectCount() { return correctCount; }
    public void setCorrectCount(int correctCount) { this.correctCount = correctCount; }
    public int getWrongCount() { return wrongCount; }
    public void setWrongCount(int wrongCount) { this.wrongCount = wrongCount; }
    public double getPercentage() { return percentage; }
    public void setPercentage(double percentage) { this.percentage = percentage; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
    public List<AnswerRecord> getAnswers() { return answers; }
    public void setAnswers(List<AnswerRecord> answers) { this.answers = answers; }
    public String getPerformanceMessage() { return performanceMessage; }
    public void setPerformanceMessage(String performanceMessage) { this.performanceMessage = performanceMessage; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }

    @Override
    public String toString() {
        return "ExamResultResponse{" +
                "examId='" + examId + '\'' +
                ", examName='" + examName + '\'' +
                ", totalQuestions=" + totalQuestions +
                ", correctCount=" + correctCount +
                ", wrongCount=" + wrongCount +
                ", percentage=" + percentage +
                ", score=" + score +
                ", status='" + status + '\'' +
                ", submittedAt=" + submittedAt +
                ", answers=" + answers +
                ", performanceMessage='" + performanceMessage + '\'' +
                ", grade='" + grade + '\'' +
                ", studentName='" + studentName + '\'' +
                ", studentEmail='" + studentEmail + '\'' +
                '}';
    }

    // Manual Builder
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String examId;
        private String examName;
        private int totalQuestions;
        private int correctCount;
        private int wrongCount;
        private double percentage;
        private int score;
        private String status;
        private Instant submittedAt;
        private List<AnswerRecord> answers;
        private String performanceMessage;
        private String grade;
        private String studentName;
        private String studentEmail;

        public Builder examId(String examId) { this.examId = examId; return this; }
        public Builder examName(String examName) { this.examName = examName; return this; }
        public Builder totalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; return this; }
        public Builder correctCount(int correctCount) { this.correctCount = correctCount; return this; }
        public Builder wrongCount(int wrongCount) { this.wrongCount = wrongCount; return this; }
        public Builder percentage(double percentage) { this.percentage = percentage; return this; }
        public Builder score(int score) { this.score = score; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder submittedAt(Instant submittedAt) { this.submittedAt = submittedAt; return this; }
        public Builder answers(List<AnswerRecord> answers) { this.answers = answers; return this; }
        public Builder performanceMessage(String performanceMessage) { this.performanceMessage = performanceMessage; return this; }
        public Builder grade(String grade) { this.grade = grade; return this; }
        public Builder studentName(String studentName) { this.studentName = studentName; return this; }
        public Builder studentEmail(String studentEmail) { this.studentEmail = studentEmail; return this; }

        public ExamResultResponse build() {
            return new ExamResultResponse(examId, examName, totalQuestions, correctCount, wrongCount,
                    percentage, score, status, submittedAt, answers, performanceMessage,
                    grade, studentName, studentEmail);
        }
    }
}
