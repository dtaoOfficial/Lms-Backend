package com.dtao.lms.payload;

public class VerifyEmailRequest {
    private String email;
    private String otp;

    public VerifyEmailRequest() {}

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }
}
