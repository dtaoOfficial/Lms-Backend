package com.dtao.lms.controller;

import com.dtao.lms.dto.ActionResponse;
import com.dtao.lms.model.Chapter;
import com.dtao.lms.model.Course;
import com.dtao.lms.model.TargetType;
import com.dtao.lms.model.Video;
import com.dtao.lms.service.CourseService;
import com.dtao.lms.service.LikeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/courses")
@CrossOrigin(
        origins = "${APP_CORS_ALLOWED_ORIGINS:http://localhost:3000}",
        allowCredentials = "true",
        allowedHeaders = "*"
)public class CourseController {

    private final CourseService service;
    private final LikeService likeService;

    public CourseController(CourseService service, LikeService likeService) {
        this.service = service;
        this.likeService = likeService;
    }

    // --- COURSE LIST (includes likes/dislikes/userState per course for current user) ---
    @GetMapping
    public ResponseEntity<?> getAllCourses() {
        List<Course> courses = service.getAllCourses();

        // try to get current user email (may be null)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = (auth != null && auth.isAuthenticated()) ? auth.getName() : null;

        List<Map<String, Object>> out = courses.stream().map(c -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("title", c.getTitle());
            m.put("description", c.getDescription());
            m.put("tags", c.getTags());
            m.put("instructor", c.getInstructor());
            m.put("duration", c.getDuration());
            // send thumbnailUrl key so frontend gets thumbnailUrl consistently
            m.put("thumbnailUrl", c.getThumbnailUrl());
            m.put("rating", c.getRating());
            m.put("enrolledStudents", c.getEnrolledStudents());
            // compute likes/dislikes/userState via LikeService
            ActionResponse stats = likeService.getStats(TargetType.COURSE, c.getId(), email);
            m.put("likes", stats.getLikes());
            m.put("dislikes", stats.getDislikes());
            m.put("userState", stats.getUserState());
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(out);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCourseById(@PathVariable String id) {
        return service.getCourseById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createCourse(@RequestBody Course course) {
        Course saved = service.createCourse(course);
        return ResponseEntity.status(201).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCourse(@PathVariable String id, @RequestBody Course data) {
        return service.updateCourse(id, data)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCourse(@PathVariable String id) {
        service.deleteCourse(id);
        return ResponseEntity.ok(Map.of("message", "Course deleted"));
    }

    // --- CHAPTERS ---
    @GetMapping("/{courseId}/chapters")
    public List<Chapter> getChapters(@PathVariable String courseId) {
        return service.getChaptersByCourse(courseId);
    }

    @PostMapping("/{courseId}/chapters")
    public ResponseEntity<?> createChapter(@PathVariable String courseId, @RequestBody Chapter chapter) {
        chapter.setCourseId(courseId);
        Chapter saved = service.createChapter(chapter);
        return ResponseEntity.status(201).body(saved);
    }

    @PutMapping("/chapters/{id}")
    public ResponseEntity<?> updateChapter(@PathVariable String id, @RequestBody Chapter data) {
        return service.updateChapter(id, data)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/chapters/{id}")
    public ResponseEntity<?> deleteChapter(@PathVariable String id) {
        service.deleteChapter(id);
        return ResponseEntity.ok(Map.of("message", "Chapter deleted"));
    }

    // --- VIDEOS (chapter-scoped) ---
    @GetMapping("/chapters/{chapterId}/videos")
    public List<Video> getVideos(@PathVariable String chapterId) {
        return service.getVideosByChapter(chapterId);
    }

    @PostMapping("/chapters/{chapterId}/videos")
    public ResponseEntity<?> createVideo(@PathVariable String chapterId, @RequestBody Video video) {
        video.setChapterId(chapterId);
        Video saved = service.createVideo(video);
        return ResponseEntity.status(201).body(saved);
    }

    @PutMapping("/videos/{id}")
    public ResponseEntity<?> updateVideo(@PathVariable String id, @RequestBody Video data) {
        return service.updateVideo(id, data)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/videos/{id}")
    public ResponseEntity<?> deleteVideo(@PathVariable String id) {
        service.deleteVideo(id);
        return ResponseEntity.ok(Map.of("message", "Video deleted"));
    }

    // --- PUBLIC Course Full (course + videos grouped by chapter) ---
    @GetMapping("/{id}/full")
    public ResponseEntity<?> getCourseFull(@PathVariable String id) {
        Optional<Course> maybe = service.getCourseById(id);
        if (maybe.isEmpty()) return ResponseEntity.status(404).body(Map.of("error", "Course not found"));
        Course course = maybe.get();

        // get ordered chapters for this course
        List<Chapter> chapters = service.getChaptersByCourse(id);

        // build chapters list with videos for each chapter (in order)
        List<Map<String, Object>> chaptersWithVids = new ArrayList<>();
        Map<String, List<Video>> videosByChapter = new HashMap<>();

        for (Chapter ch : chapters) {
            List<Video> vids = service.getVideosByChapter(ch.getId());
            chaptersWithVids.add(Map.of("chapter", ch, "videos", vids));
            videosByChapter.put(ch.getId(), vids);
        }

        // include videos without chapter (General)
        List<Video> uncategorized = service.getVideosWithoutChapterForCourse(id);
        if (uncategorized != null && !uncategorized.isEmpty()) {
            chaptersWithVids.add(Map.of("chapter", Map.of("id", "", "title", "General"), "videos", uncategorized));
            videosByChapter.put("", uncategorized);
        }

        // include likes/dislikes/userState for course (if auth)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = (auth != null && auth.isAuthenticated()) ? auth.getName() : null;
        ActionResponse courseStats = likeService.getStats(TargetType.COURSE, id, email);

        Map<String, Object> resp = new HashMap<>();
        resp.put("course", course);
        resp.put("chapters", chaptersWithVids);
        resp.put("videosByChapter", videosByChapter);
        resp.put("likes", courseStats.getLikes());
        resp.put("dislikes", courseStats.getDislikes());
        resp.put("userState", courseStats.getUserState());
        return ResponseEntity.ok(resp);
    }
}
