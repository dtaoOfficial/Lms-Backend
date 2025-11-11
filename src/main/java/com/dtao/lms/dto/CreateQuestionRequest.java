package com.dtao.lms.dto;

public class CreateQuestionRequest {
    private String courseId;
    private String questionText;

    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
}
