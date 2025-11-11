package com.dtao.lms.controller;

import com.dtao.lms.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private AdminService adminService;

    // 🧮 Get overall dashboard statistics
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = adminService.getDashboardStats();
        return ResponseEntity.ok(stats);
    }

    // 📈 Get enrollment analytics for charts
    @GetMapping("/analytics/enrollments")
    public ResponseEntity<Map<String, Object>> getEnrollmentAnalytics() {
        Map<String, Object> data = adminService.getEnrollmentAnalytics();
        return ResponseEntity.ok(data);
    }

    // 🎓 Get course engagement analytics
    @GetMapping("/analytics/course-engagement")
    public ResponseEntity<Map<String, Object>> getCourseEngagement() {
        Map<String, Object> data = adminService.getCourseEngagement();
        return ResponseEntity.ok(data);
    }
}
