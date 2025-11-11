package com.dtao.lms.controller;

import com.dtao.lms.model.Course;
import com.dtao.lms.model.Enrollment;
import com.dtao.lms.model.Video;
import com.dtao.lms.repo.CourseRepository;
import com.dtao.lms.repo.EnrollmentRepository;
import com.dtao.lms.repo.VideoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(
        origins = "${APP_CORS_ALLOWED_ORIGINS:http://localhost:3000}",
        allowCredentials = "true",
        allowedHeaders = "*"
)
public class StudentCourseController {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final VideoRepository videoRepository;

    public StudentCourseController(EnrollmentRepository enrollmentRepository,
                                   CourseRepository courseRepository,
                                   VideoRepository videoRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.courseRepository = courseRepository;
        this.videoRepository = videoRepository;
    }

    // GET /api/student/courses -> return approved/enrolled course list for current user
    @GetMapping("/student/courses")
    public ResponseEntity<?> getMyCourses() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || auth.getName() == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));

            String email = auth.getName();
            List<Enrollment> enrollments = enrollmentRepository.findByEmailAndStatus(email, "APPROVED");
            List<String> courseIds = enrollments.stream().map(Enrollment::getCourseId).filter(Objects::nonNull).collect(Collectors.toList());
            List<Course> courses = courseRepository.findAllById(courseIds);
            return ResponseEntity.ok(courses);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Server error"));
        }
    }

    /**
     * Student-specific full course details
     * GET /api/student/courses/{id}/full
     *
     * NOTE: this route intentionally differs from the public /api/courses/{id}/full
     * to avoid ambiguous mapping with the EnrollmentController public endpoint.
     */
    @GetMapping("/student/courses/{id}/full")
    public ResponseEntity<?> getStudentCourseFull(@PathVariable String id) {
        try {
            Optional<Course> maybe = courseRepository.findById(id);
            if (maybe.isEmpty()) return ResponseEntity.status(404).body(Map.of("error", "Course not found"));

            Course course = maybe.get();
            // gather videos for this course
            List<Video> videos = videoRepository.findAll()
                    .stream()
                    .filter(v -> id.equals(v.getCourseId()))
                    .collect(Collectors.toList());

            // group by chapterId (empty string for no chapter)
            Map<String, List<Video>> byChapter = videos.stream()
                    .collect(Collectors.groupingBy(v -> v.getChapterId() == null ? "" : v.getChapterId()));

            Map<String, Object> resp = new HashMap<>();
            resp.put("course", course);
            resp.put("videosByChapter", byChapter);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Server error"));
        }
    }
}
