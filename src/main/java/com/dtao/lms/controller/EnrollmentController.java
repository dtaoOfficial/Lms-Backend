package com.dtao.lms.controller;

import com.dtao.lms.model.Chapter;
import com.dtao.lms.model.Course;
import com.dtao.lms.model.Enrollment;
import com.dtao.lms.model.Video;
import com.dtao.lms.service.CourseService;
import com.dtao.lms.service.EnrollmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * EnrollmentController
 *
 * Enriched endpoints for frontend convenience.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(
        origins = "${APP_CORS_ALLOWED_ORIGINS:http://localhost:3000}",
        allowCredentials = "true",
        allowedHeaders = "*"
)
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final CourseService courseService;

    public EnrollmentController(EnrollmentService enrollmentService, CourseService courseService) {
        this.enrollmentService = enrollmentService;
        this.courseService = courseService;
    }

    // Student: enroll in course (authenticated)
    @PostMapping("/courses/{courseId}/enroll")
    public ResponseEntity<?> enrollInCourse(@PathVariable String courseId) {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }
            String email = auth.getName();
            String userId = null;
            Object principal = auth.getPrincipal();
            if (principal instanceof com.dtao.lms.security.CustomUserDetails) {
                userId = ((com.dtao.lms.security.CustomUserDetails) principal).getId();
            }
            Enrollment created = enrollmentService.createEnrollmentRequest(courseId, email, userId);
            return ResponseEntity.status(201).body(created);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Server error"));
        }
    }

    // Student: get my enrollment status for a course
    @GetMapping("/courses/{courseId}/enrollment")
    public ResponseEntity<?> getMyEnrollment(@PathVariable String courseId) {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }
            String email = auth.getName();
            Optional<Enrollment> maybe = enrollmentService.getEnrollmentForUser(courseId, email);
            if (maybe.isEmpty()) return ResponseEntity.ok(Map.of("status", "NONE"));
            return ResponseEntity.ok(maybe.get());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Server error"));
        }
    }

    // Student -> get all my enrollments (enriched)
    @GetMapping("/enrollments/me")
    public ResponseEntity<?> getMyEnrollments() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }
            String email = auth.getName();

            List<Enrollment> list = enrollmentService.getEnrollmentsByEmail(email);
            if (list == null) list = Collections.emptyList();

            // Enrich each enrollment with course metadata
            List<Map<String, Object>> enriched = new ArrayList<>();
            for (Enrollment e : list) {
                Map<String, Object> map = new HashMap<>();
                map.put("enrollment", e);

                try {
                    Optional<Course> maybeCourse = courseService.getCourseById(e.getCourseId());
                    if (maybeCourse.isPresent()) {
                        Course c = maybeCourse.get();
                        map.put("courseTitle", c.getTitle());
                        map.put("courseSummary", c.getDescription());
                        // attempt thumbnail via reflection (optional)
                        try {
                            var thumbField = c.getClass().getMethod("getThumbnail");
                            Object thumb = thumbField.invoke(c);
                            map.put("courseThumbnail", thumb);
                        } catch (NoSuchMethodException ignored) {
                            map.put("courseThumbnail", null);
                        } catch (Exception ex) {
                            map.put("courseThumbnail", null);
                        }

                        // total videos count
                        try {
                            List<Video> vids = courseService.getAllVideosForCourse(c.getId());
                            map.put("totalVideos", vids == null ? 0 : vids.size());
                        } catch (Exception ex) {
                            map.put("totalVideos", 0);
                        }
                    } else {
                        map.put("courseTitle", null);
                        map.put("courseSummary", null);
                        map.put("courseThumbnail", null);
                        map.put("totalVideos", 0);
                    }
                } catch (Exception ex) {
                    map.put("courseTitle", null);
                    map.put("courseSummary", null);
                    map.put("courseThumbnail", null);
                    map.put("totalVideos", 0);
                }

                enriched.add(map);
            }

            return ResponseEntity.ok(enriched);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Server error"));
        }
    }

    // Enrollment-specific "full" view (optional)
    @GetMapping("/enrollments/course/{courseId}/full")
    public ResponseEntity<?> getCourseFullForEnrollment(@PathVariable String courseId) {
        try {
            var maybe = courseService.getCourseById(courseId);
            if (maybe.isEmpty()) return ResponseEntity.notFound().build();
            var course = maybe.get();

            List<Chapter> chapters = courseService.getChaptersByCourse(courseId);
            List<Map<String, Object>> chaptersWithVids = new ArrayList<>();
            Map<String, List<Video>> videosByChapter = new HashMap<>();

            for (Chapter ch : chapters) {
                List<Video> vids = courseService.getVideosByChapter(ch.getId());
                chaptersWithVids.add(Map.of("chapter", ch, "videos", vids));
                videosByChapter.put(ch.getId(), vids);
            }

            // include videos without chapter
            List<Video> uncategorized = courseService.getVideosWithoutChapterForCourse(courseId);
            if (uncategorized != null && !uncategorized.isEmpty()) {
                chaptersWithVids.add(Map.of("chapter", Map.of("id", "", "title", "General"), "videos", uncategorized));
                videosByChapter.put("", uncategorized);
            }

            Map<String, Object> resp = new HashMap<>();
            resp.put("course", course);
            resp.put("chapters", chaptersWithVids);
            resp.put("videosByChapter", videosByChapter);

            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Server error"));
        }
    }
}
