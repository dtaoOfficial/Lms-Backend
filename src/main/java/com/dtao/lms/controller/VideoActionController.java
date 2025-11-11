package com.dtao.lms.controller;

import com.dtao.lms.dto.ActionResponse;
import com.dtao.lms.dto.CreateCommentRequest;
import com.dtao.lms.dto.CreateReportRequest;
import com.dtao.lms.dto.PagedCommentsResponse;
import com.dtao.lms.model.Comment;
import com.dtao.lms.model.TargetType;
import com.dtao.lms.service.CommentService;
import com.dtao.lms.service.CourseService;
import com.dtao.lms.service.LikeService;
import com.dtao.lms.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/videos")
@CrossOrigin(
        origins = "${APP_CORS_ALLOWED_ORIGINS:http://localhost:3000}",
        allowCredentials = "true",
        allowedHeaders = "*"
)
public class VideoActionController {

    private final LikeService likeService;
    private final CommentService commentService;
    private final ReportService reportService;
    private final CourseService courseService;

    @Autowired
    public VideoActionController(LikeService likeService, CommentService commentService, ReportService reportService, CourseService courseService) {
        this.likeService = likeService;
        this.commentService = commentService;
        this.reportService = reportService;
        this.courseService = courseService;
    }

    private String getCurrentUserEmail() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated()) return null;
        Object principal = a.getPrincipal();
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            // try reflection for getEmail (some custom details expose email)
            try {
                java.lang.reflect.Method m = principal.getClass().getMethod("getEmail");
                Object r = m.invoke(principal);
                if (r != null) return r.toString();
            } catch (Exception ignored) {}
            return ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
        }
        return principal.toString();
    }

    /**
     * GET /api/videos/{id}/stats
     * Return basic video stats: like/dislike counts and userState (if authenticated).
     */
    @GetMapping("/{id}/stats")
    public ResponseEntity<?> getVideoWithStats(@PathVariable String id) {
        try {
            String email = getCurrentUserEmail();
            ActionResponse stats = likeService.getStats(TargetType.VIDEO, id, email);
            return ResponseEntity.ok(Map.of(
                    "id", id,
                    "likes", stats.getLikes(),
                    "dislikes", stats.getDislikes(),
                    "userState", stats.getUserState()
            ));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Server error"));
        }
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<?> likeVideo(@PathVariable String id) {
        String email = getCurrentUserEmail();
        if (email == null) return ResponseEntity.status(401).body(Map.of("error","Unauthorized"));
        ActionResponse resp = likeService.toggleLike(TargetType.VIDEO, id, email);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/{id}/dislike")
    public ResponseEntity<?> dislikeVideo(@PathVariable String id) {
        String email = getCurrentUserEmail();
        if (email == null) return ResponseEntity.status(401).body(Map.of("error","Unauthorized"));
        ActionResponse resp = likeService.toggleDislike(TargetType.VIDEO, id, email);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/{id}/comment")
    public ResponseEntity<?> createComment(@PathVariable String id, @RequestBody CreateCommentRequest req) {
        String email = getCurrentUserEmail();
        if (email == null) return ResponseEntity.status(401).body(Map.of("error","Unauthorized"));
        try {
            Comment c = commentService.createComment(TargetType.VIDEO, id, email, req.getText());
            return ResponseEntity.status(201).body(c);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<?> listComments(@PathVariable String id,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        List<Comment> list = commentService.listComments(TargetType.VIDEO, id, page, size);
        long total = commentService.countComments(TargetType.VIDEO, id);
        return ResponseEntity.ok(new PagedCommentsResponse(page, size, total, list));
    }

    @PostMapping("/{id}/report")
    public ResponseEntity<?> reportVideo(@PathVariable String id, @RequestBody CreateReportRequest req) {
        String email = getCurrentUserEmail();
        if (email == null) return ResponseEntity.status(401).body(Map.of("error","Unauthorized"));
        try {
            var r = reportService.createReport(TargetType.VIDEO, id, email, req.getReason(), req.getText());
            return ResponseEntity.status(201).body(Map.of("reportId", r.getId()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
}
