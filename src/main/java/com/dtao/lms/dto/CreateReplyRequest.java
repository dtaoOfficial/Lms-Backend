package com.dtao.lms.dto;

public class CreateReplyRequest {
    private String questionId;
    private String replyText;

    public String getQuestionId() { return questionId; }
    public void setQuestionId(String questionId) { this.questionId = questionId; }

    public String getReplyText() { return replyText; }
    public void setReplyText(String replyText) { this.replyText = replyText; }
}
