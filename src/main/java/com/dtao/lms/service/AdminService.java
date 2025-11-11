package com.dtao.lms.service;

import com.dtao.lms.model.Course;
import com.dtao.lms.model.Enrollment;
import com.dtao.lms.model.User;
import com.dtao.lms.repo.CourseRepository;
import com.dtao.lms.repo.EnrollmentRepository;
import com.dtao.lms.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    // ==============================
    // DASHBOARD STATS
    // ==============================
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        long totalUsers = userRepository.count();
        long totalStudents = userRepository.countByRole(User.Roles.STUDENT);
        long totalTeachers = userRepository.countByRole(User.Roles.TEACHER);
        long totalAdmins = userRepository.countByRole(User.Roles.ADMIN);

        long totalCourses = courseRepository.count();
        long totalEnrollments = enrollmentRepository.count();

        long pendingRequests = enrollmentRepository.countByStatus("PENDING");
        long approvedRequests = enrollmentRepository.countByStatus("APPROVED");

        stats.put("totalUsers", totalUsers);
        stats.put("totalStudents", totalStudents);
        stats.put("totalTeachers", totalTeachers);
        stats.put("totalAdmins", totalAdmins);
        stats.put("totalCourses", totalCourses);
        stats.put("totalEnrollments", totalEnrollments);
        stats.put("pendingRequests", pendingRequests);
        stats.put("approvedRequests", approvedRequests);
        stats.put("timestamp", Instant.now());

        return stats;
    }

    // ==============================
    // ENROLLMENT ANALYTICS
    // ==============================
    public Map<String, Object> getEnrollmentAnalytics() {
        Map<String, Object> response = new LinkedHashMap<>();

        Instant now = Instant.now();
        Instant last30Days = now.minus(30, ChronoUnit.DAYS);

        List<Enrollment> enrollments = enrollmentRepository.findByCreatedAtAfter(last30Days);

        Map<String, Long> dailyEnrollments = enrollments.stream()
                .collect(Collectors.groupingBy(e ->
                        e.getCreatedAt().truncatedTo(ChronoUnit.DAYS).toString(),
                        Collectors.counting()
                ));

        response.put("period", "last 30 days");
        response.put("data", dailyEnrollments);
        response.put("total", enrollments.size());
        return response;
    }

    // ==============================
    // COURSE ENGAGEMENT ANALYTICS
    // ==============================
    public Map<String, Object> getCourseEngagement() {
        Map<String, Object> response = new LinkedHashMap<>();

        List<Course> courses = courseRepository.findAll();

        List<Map<String, Object>> engagementList = courses.stream()
                .map(course -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("title", course.getTitle());
                    item.put("enrolledStudents", course.getEnrolledStudents());
                    item.put("rating", course.getRating() != null ? course.getRating() : 0);
                    return item;
                })
                .collect(Collectors.toList());

        response.put("totalCourses", courses.size());
        response.put("courses", engagementList);

        return response;
    }
}
