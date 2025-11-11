package com.dtao.lms.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("enrollment_audits")
public class EnrollmentAudit {

    @Id
    private String id;
    private String enrollmentId;
    private String courseId;
    private String studentEmail;
    private String action; // APPROVED / REJECTED
    private String note;
    private String adminEmail;
    private Instant timestamp;

    public EnrollmentAudit() {}

    public EnrollmentAudit(String enrollmentId, String courseId, String studentEmail,
                           String action, String note, String adminEmail, Instant timestamp) {
        this.enrollmentId = enrollmentId;
        this.courseId = courseId;
        this.studentEmail = studentEmail;
        this.action = action;
        this.note = note;
        this.adminEmail = adminEmail;
        this.timestamp = timestamp;
    }

    // getters/setters
    public String getId() { return id; }
    public String getEnrollmentId() { return enrollmentId; }
    public void setEnrollmentId(String enrollmentId) { this.enrollmentId = enrollmentId; }
    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }
    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getAdminEmail() { return adminEmail; }
    public void setAdminEmail(String adminEmail) { this.adminEmail = adminEmail; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
