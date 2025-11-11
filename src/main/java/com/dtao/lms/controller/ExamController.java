package com.dtao.lms.controller;

import com.dtao.lms.model.Exam;
import com.dtao.lms.service.ExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Handles CRUD operations for Exams (Admin Side)
 */
@RestController
@RequestMapping("/api/admin/exams")
@CrossOrigin(
        origins = "${APP_CORS_ALLOWED_ORIGINS:http://localhost:3000}",
        allowCredentials = "true",
        allowedHeaders = "*"
)
public class ExamController {

    private final ExamService examService;

    @Autowired
    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    /**
     * Create new exam
     */
    @PostMapping
    public ResponseEntity<Exam> createExam(@RequestBody Exam exam) {
        Exam savedExam = examService.createExam(exam);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedExam);
    }

    /**
     * Get all exams
     */
    @GetMapping
    public ResponseEntity<List<Exam>> getAllExams() {
        List<Exam> exams = examService.getAllExams();
        return ResponseEntity.ok(exams);
    }

    /**
     * Get exam by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Exam> getExamById(@PathVariable String id) {
        Exam exam = examService.getExamById(id);
        return ResponseEntity.ok(exam);
    }

    /**
     * Update exam details
     */
    @PutMapping("/{id}")
    public ResponseEntity<Exam> updateExam(@PathVariable String id, @RequestBody Exam updatedExam) {
        Exam exam = examService.updateExam(id, updatedExam);
        return ResponseEntity.ok(exam);
    }

    /**
     * Toggle publish/unpublish
     */
    @PatchMapping("/{id}/publish")
    public ResponseEntity<Exam> togglePublish(
            @PathVariable String id,
            @RequestParam("publish") boolean publish
    ) {
        Exam exam = examService.togglePublish(id, publish);
        return ResponseEntity.ok(exam);
    }

    /**
     * Delete exam by ID
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExam(@PathVariable String id) {
        examService.deleteExam(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get only published exams (for student visibility)
     */
    @GetMapping("/published")
    public ResponseEntity<List<Exam>> getPublishedExams() {
        List<Exam> exams = examService.getPublishedExams();
        return ResponseEntity.ok(exams);
    }

    /**
     * Get currently active exams based on date window
     */
    @GetMapping("/active")
    public ResponseEntity<List<Exam>> getActiveExams() {
        List<Exam> exams = examService.getActiveExams();
        return ResponseEntity.ok(exams);
    }
}
