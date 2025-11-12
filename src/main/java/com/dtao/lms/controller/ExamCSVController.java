package com.dtao.lms.controller;

import com.dtao.lms.model.Exam;
import com.dtao.lms.service.ExamCSVService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * ✅ ExamCSVController (Final Production Version)
 * Handles CSV upload for exam question imports with validation, logging, and
 * structured JSON responses.
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
     * ✅ Upload CSV file to import MCQ questions into an existing exam.
     * Expected CSV header:
     * Question,OptionA,OptionB,OptionC,OptionD,Answer,Explanation
     */
    @PostMapping("/{examId}/upload")
    public ResponseEntity<Map<String, Object>> uploadExamCSV(
            @PathVariable("examId") String examId,
            @RequestParam("file") MultipartFile file
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 🧩 Validate exam ID
            if (examId == null || examId.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "❌ Invalid request. Missing exam ID.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // 🧩 Validate file
            if (file == null || file.isEmpty()) {
                response.put("success", false);
                response.put("message", "❌ No file uploaded. Please select a valid CSV file.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            String filename = file.getOriginalFilename() != null
                    ? file.getOriginalFilename().toLowerCase()
                    : "";

            if (!filename.endsWith(".csv")) {
                response.put("success", false);
                response.put("message", "⚠️ Invalid file type. Please upload a .csv file only.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // 🚀 Process CSV
            Exam updatedExam = examCSVService.uploadQuestionsFromCSV(examId, file);

            response.put("success", true);
            response.put("message", String.format(
                    "✅ CSV uploaded successfully — %d questions imported into '%s'.",
                    updatedExam.getQuestions().size(),
                    updatedExam.getName()
            ));
            response.put("examId", updatedExam.getId());
            response.put("examName", updatedExam.getName());
            response.put("questionCount", updatedExam.getQuestions().size());

            System.out.printf("✅ CSV import successful: %d questions added to exam '%s' (ID: %s)%n",
                    updatedExam.getQuestions().size(), updatedExam.getName(), updatedExam.getId());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            // 💥 Known application errors (bad CSV, invalid headers, etc.)
            System.err.println("❌ CSV upload failed: " + e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (Exception e) {
            // 💀 Unexpected system errors
            System.err.println("💥 Unexpected error while uploading CSV: " + e.getMessage());
            response.put("success", false);
            response.put("message", "💥 Server error while processing the CSV. Please try again later.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
