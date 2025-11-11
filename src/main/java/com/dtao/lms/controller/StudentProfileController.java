package com.dtao.lms.controller;

import com.dtao.lms.model.User;
import com.dtao.lms.payload.ChangePasswordRequest;
import com.dtao.lms.payload.UpdateProfileRequest;
import com.dtao.lms.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/student")
@CrossOrigin(
        origins = "${APP_CORS_ALLOWED_ORIGINS:http://localhost:3000}",
        allowCredentials = "true",
        allowedHeaders = "*"
)public class StudentProfileController {

    @Autowired
    private UserService userService;

    // ============================
    // Get Current Student Profile
    // ============================
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        String email = userDetails.getUsername();
        var userOpt = userService.getUserByEmail(email);

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }

        User user = userOpt.get();
        Map<String, Object> data = new HashMap<>();
        data.put("name", user.getName());
        data.put("email", user.getEmail());
        data.put("phone", user.getPhone());
        data.put("department", user.getDepartment());
        data.put("role", user.getRole());
        data.put("about", user.getAbout());
        data.put("createdAt", user.getCreatedAt());
        data.put("updatedAt", user.getUpdatedAt());

        return ResponseEntity.ok(data);
    }

    // ============================
    // Update Profile (Non-Password)
    // ============================
    @PatchMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UpdateProfileRequest request) {

        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        String email = userDetails.getUsername();

        try {
            User updated = userService.updateStudentProfile(email, request);
            return ResponseEntity.ok(Map.of(
                    "message", "Profile updated successfully",
                    "user", updated
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ============================
    // Change Password
    // ============================
    @PatchMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody ChangePasswordRequest request) {

        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        String email = userDetails.getUsername();

        try {
            userService.changePassword(email, request.getOldPassword(), request.getNewPassword());
            return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
}
