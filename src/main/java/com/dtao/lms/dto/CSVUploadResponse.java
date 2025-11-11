package com.dtao.lms.dto;

import java.util.List;

public class CSVUploadResponse {

    private String examId;
    private int questionCount;
    private List<String> errors;
    private boolean success;

    public CSVUploadResponse() {}

    public CSVUploadResponse(String examId, int questionCount, List<String> errors, boolean success) {
        this.examId = examId;
        this.questionCount = questionCount;
        this.errors = errors;
        this.success = success;
    }

    public String getExamId() { return examId; }
    public void setExamId(String examId) { this.examId = examId; }

    public int getQuestionCount() { return questionCount; }
    public void setQuestionCount(int questionCount) { this.questionCount = questionCount; }

    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
}
