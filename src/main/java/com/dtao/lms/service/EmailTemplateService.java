package com.dtao.lms.service;

import org.springframework.stereotype.Service;

@Service
public class EmailTemplateService {

    private String wrap(String title, String body) {
        return String.format("""
                <div style="font-family:'Poppins',sans-serif;padding:20px;background-color:#f7f8fa;">
                  <div style="max-width:600px;margin:auto;background:white;border-radius:12px;box-shadow:0 2px 10px rgba(0,0,0,0.05);overflow:hidden;">
                    <div style="background:#4A90E2;color:white;padding:16px;font-size:20px;font-weight:bold;">
                      DTAO Learning Portal
                    </div>
                    <div style="padding:20px;color:#333;">
                      <h2 style="color:#4A90E2;">%s</h2>
                      <p style="font-size:16px;">%s</p>
                    </div>
                    <div style="padding:16px;text-align:center;font-size:13px;color:#888;border-top:1px solid #eee;">
                      © 2025 DTAO Official |
                      <a href="https://dtaoofficial.netlify.app" style="color:#4A90E2;text-decoration:none;">Visit Website</a>
                    </div>
                  </div>
                </div>
                """, title, body);
    }

    public String buildWelcomeTemplate(String name) {
        return wrap("Welcome " + name + " 🎉",
                "We’re thrilled to have you on board! Start learning and explore your courses today.");
    }

    public String buildNewCourseTemplate(String title, String desc) {
        return wrap("New Course: " + title,
                "A new course has been added: <b>" + title + "</b><br><br>" + desc);
    }

    public String buildEnrollmentTemplate(String name, String course) {
        return wrap("Enrollment Successful ✅",
                "Hi " + name + ", you have successfully enrolled in <b>" + course + "</b>.<br>Start learning now!");
    }

    public String buildApprovalTemplate(String name, String course) {
        return wrap("Your Enrollment Approved 🎯",
                "Hi " + name + ", your enrollment for <b>" + course + "</b> has been approved. Enjoy learning!");
    }

    public String buildNewVideoTemplate(String course, String videoTitle) {
        return wrap("New Lesson Added 📹",
                "A new video <b>" + videoTitle + "</b> was added to your course <b>" + course + "</b>.");
    }

    public String buildRemovalTemplate(String name, String course) {
        return wrap("Removed from Course ⚠",
                "Hi " + name + ", you have been removed from <b>" + course + "</b>.<br>If you think this was a mistake, please contact support.");
    }
}
