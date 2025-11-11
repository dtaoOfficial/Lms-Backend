package com.dtao.lms.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Document(collection = "users")
public class User {

    @Id
    private String id;

    private String name;
    private String department;
    private String email;
    private String phone;
    private String passwordHash;
    private String role;

    private boolean isVerified = false;
    private int failedLoginAttempts = 0;
    private Instant lockedUntil;

    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;

    private boolean active = true;

    // ✅ About section
    private String about;

    // ✅ Profile image fields
    private String profileImage;

    @Field("profileImageData")
    private byte[] profileImageData;

    @Field("profileImageType")
    private String profileImageType;

    // ✅ NEW FIELDS for dashboard analytics
    private long totalLikesGiven = 0;
    private long totalCertificatesEarned = 0;
    private double averageProgressPercent = 0.0;

    // ==============================
    // Role constants
    // ==============================
    public static final class Roles {
        public static final String ADMIN = "ADMIN";
        public static final String TEACHER = "TEACHER";
        public static final String STUDENT = "STUDENT";
        public static final String DEPARTMENT = "DEPARTMENT";
    }

    // ==============================
    // Getters and Setters
    // ==============================
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email == null ? null : email.trim().toLowerCase(); }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getRole() { return role; }
    public void setRole(String role) {
        if (role == null || role.isBlank()) {
            this.role = Roles.STUDENT;
        } else {
            this.role = role.trim().toUpperCase();
        }
    }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }

    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public void setFailedLoginAttempts(int failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }

    public Instant getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(Instant lockedUntil) { this.lockedUntil = lockedUntil; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getAbout() { return about; }
    public void setAbout(String about) { this.about = about; }

    public String getProfileImage() { return profileImage; }
    public void setProfileImage(String profileImage) { this.profileImage = profileImage; }

    public byte[] getProfileImageData() { return profileImageData; }
    public void setProfileImageData(byte[] profileImageData) { this.profileImageData = profileImageData; }

    public String getProfileImageType() { return profileImageType; }
    public void setProfileImageType(String profileImageType) { this.profileImageType = profileImageType; }

    // ✅ NEW GETTERS & SETTERS for dashboard fields
    public long getTotalLikesGiven() { return totalLikesGiven; }
    public void setTotalLikesGiven(long totalLikesGiven) { this.totalLikesGiven = totalLikesGiven; }

    public long getTotalCertificatesEarned() { return totalCertificatesEarned; }
    public void setTotalCertificatesEarned(long totalCertificatesEarned) { this.totalCertificatesEarned = totalCertificatesEarned; }

    public double getAverageProgressPercent() { return averageProgressPercent; }
    public void setAverageProgressPercent(double averageProgressPercent) { this.averageProgressPercent = averageProgressPercent; }


}
