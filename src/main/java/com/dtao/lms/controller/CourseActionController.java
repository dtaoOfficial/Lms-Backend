package com.dtao.lms.controller;

import com.dtao.lms.dto.ActionResponse;
import com.dtao.lms.dto.CreateCommentRequest;
import com.dtao.lms.dto.CreateReportRequest;
import com.dtao.lms.dto.PagedCommentsResponse;
import com.dtao.lms.model.Comment;
import com.dtao.lms.model.TargetType;
import com.dtao.lms.service.CommentService;
import com.dtao.lms.service.LikeService;
import com.dtao.lms.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses/{courseId}")
@CrossOrigin(
        origins = "${APP_CORS_ALLOWED_ORIGINS:http://localhost:3000}",
        allowCredentials = "true",
        allowedHeaders = "*"
)

public class CourseActionController {

    private final LikeService likeService;
    private final CommentService commentService;
    private final ReportService reportService;

    @Autowired
    public CourseActionController(LikeService likeService, CommentService commentService, ReportService reportService) {
        this.likeService = likeService;
        this.commentService = commentService;
        this.reportService = reportService;
    }

    private String getCurrentUserEmail() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated()) return null;
        // Prefer Authentication.getName() which your Jwt filter / UserDetails sets to email
        String name = a.getName();
        if (name != null && !name.isBlank()) return name;
        // fallback to principal inspection
        Object p = a.getPrincipal();
        try {
            if (p instanceof org.springframework.security.core.userdetails.UserDetails) {
                return ((org.springframework.security.core.userdetails.UserDetails) p).getUsername();
            } else if (p instanceof String) {
                return (String) p;
            }
        } catch (Exception ignored) {}
        return null;
    }

    @PostMapping("/like")
    public ResponseEntity<?> likeCourse(@PathVariable String courseId) {
        String email = getCurrentUserEmail();
        if (email == null) return ResponseEntity.status(401).build();
        ActionResponse resp = likeService.toggleLike(TargetType.COURSE, courseId, email);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/dislike")
    public ResponseEntity<?> dislikeCourse(@PathVariable String courseId) {
        String email = getCurrentUserEmail();
        if (email == null) return ResponseEntity.status(401).build();
        ActionResponse resp = likeService.toggleDislike(TargetType.COURSE, courseId, email);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/comment")
    public ResponseEntity<?> createComment(@PathVariable String courseId, @RequestBody CreateCommentRequest req) {
        String email = getCurrentUserEmail();
        if (email == null) return ResponseEntity.status(401).build();
        Comment c = commentService.createComment(TargetType.COURSE, courseId, email, req.getText());
        return ResponseEntity.status(201).body(c);
    }

    @GetMapping("/comments")
    public ResponseEntity<?> listComments(@PathVariable String courseId,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        List<Comment> list = commentService.listComments(TargetType.COURSE, courseId, page, size);
        long total = commentService.countComments(TargetType.COURSE, courseId);
        return ResponseEntity.ok(new PagedCommentsResponse(page, size, total, list));
    }

    @PostMapping("/report")
    public ResponseEntity<?> reportCourse(@PathVariable String courseId, @RequestBody CreateReportRequest req) {
        String email = getCurrentUserEmail();
        if (email == null) return ResponseEntity.status(401).build();
        var r = reportService.createReport(TargetType.COURSE, courseId, email, req.getReason(), req.getText());
        return ResponseEntity.status(201).body(java.util.Map.of("reportId", r.getId()));
    }
}
