package com.dtao.lms.controller;

import com.dtao.lms.model.Exam;
import com.dtao.lms.service.ExamCSVService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Handles CSV upload for exam question import
 */
@RestController
@RequestMapping("/api/admin/exams")
@CrossOrigin(
        origins = "${APP_CORS_ALLOWED_ORIGINS:http://localhost:3000}",
        allowCredentials = "true",
        allowedHeaders = "*"
)
public class ExamCSVController {

    private final ExamCSVService examCSVService;

    @Autowired
    public ExamCSVController(ExamCSVService examCSVService) {
        this.examCSVService = examCSVService;
    }

    /**
     * Upload CSV file to import MCQ questions into an existing exam
     * CSV format:
     * Question,OptionA,OptionB,OptionC,OptionD,Answer,Explanation
     */
    @PostMapping("/{examId}/upload")
    public ResponseEntity<?> uploadExamCSV(
            @PathVariable("examId") String examId,
            @RequestParam("file") MultipartFile file
    ) {
        try {
            Exam updatedExam = examCSVService.uploadQuestionsFromCSV(examId, file);
            return ResponseEntity.ok(updatedExam);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error: " + e.getMessage());
        }
    }
}
