package com.dtao.lms.dto;

public class CreateReportRequest {
    private String reason;
    private String text;

    public CreateReportRequest() {}
    public CreateReportRequest(String reason, String text) { this.reason = reason; this.text = text; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}
