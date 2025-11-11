package com.dtao.lms.dto;

public class CreateCommentRequest {
    private String text;

    public CreateCommentRequest() {}
    public CreateCommentRequest(String text) { this.text = text; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}
