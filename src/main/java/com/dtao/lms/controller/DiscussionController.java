package com.dtao.lms.controller;

import com.dtao.lms.dto.CreateQuestionRequest;
import com.dtao.lms.dto.CreateReplyRequest;
import com.dtao.lms.dto.CreateReportRequest;
import com.dtao.lms.model.DiscussionQuestion;
import com.dtao.lms.model.TargetType;
import com.dtao.lms.service.DiscussionService;
import com.dtao.lms.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/forum")
public class DiscussionController {

    @Autowired
    private DiscussionService discussionService;

    @Autowired
    private ReportService reportService;

    @PostMapping("/question")
    public ResponseEntity<DiscussionQuestion> createQuestion(@RequestBody CreateQuestionRequest request,
                                                             Authentication auth) {
        String email = auth.getName();
        return ResponseEntity.ok(discussionService.createQuestion(email, request));
    }

    @PostMapping("/reply")
    public ResponseEntity<DiscussionQuestion> addReply(@RequestBody CreateReplyRequest request,
                                                       Authentication auth) {
        String email = auth.getName();
        DiscussionQuestion updated = discussionService.addReply(email, request);
        if (updated == null) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{courseId}")
    public ResponseEntity<List<DiscussionQuestion>> getQuestions(@PathVariable String courseId) {
        return ResponseEntity.ok(discussionService.getQuestionsByCourse(courseId));
    }

    // 🧩 NEW: Report a question
    @PostMapping("/question/{id}/report")
    public ResponseEntity<?> reportQuestion(@PathVariable String id,
                                            @RequestBody CreateReportRequest req,
                                            Authentication auth) {
        String email = auth.getName();
        var report = reportService.createReport(
                TargetType.FORUM_QUESTION,
                id,
                email,
                req.getReason(),
                req.getText()
        );
        return ResponseEntity.ok(Map.of("reportId", report.getId()));
    }

    // 🧩 NEW: Report a reply
    @PostMapping("/reply/{id}/report")
    public ResponseEntity<?> reportReply(@PathVariable String id,
                                         @RequestBody CreateReportRequest req,
                                         Authentication auth) {
        String email = auth.getName();
        var report = reportService.createReport(
                TargetType.FORUM_REPLY,
                id,
                email,
                req.getReason(),
                req.getText()
        );
        return ResponseEntity.ok(Map.of("reportId", report.getId()));
    }
}
