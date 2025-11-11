package com.dtao.lms.controller;

import com.dtao.lms.dto.*;
import com.dtao.lms.service.StudentExamService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * ✅ StudentExamController
 * Handles all endpoints related to student exams:
 *  - View available exams
 *  - Start exam
 *  - Get exam questions
 *  - Submit answers
 *  - Fetch exam results
 */
@RestController
@RequestMapping("/api/student/exams")
public class StudentExamController {

    private final StudentExamService studentExamService;

    // ✅ Manual constructor (replaces Lombok @RequiredArgsConstructor)
    public StudentExamController(StudentExamService studentExamService) {
        this.studentExamService = studentExamService;
    }

    /* ============================================================
     * 1️⃣ Fetch Available Exams (Includes per-student status)
     * ============================================================ */
    @GetMapping
    public ResponseEntity<List<ExamResponse>> getAvailableExams(Principal principal) {
        String email = principal.getName();
        List<ExamResponse> exams = studentExamService.getAvailableExamsForStudent(email);
        return ResponseEntity.ok(exams);
    }

    /* ============================================================
     * 2️⃣ Start Exam (Creates IN_PROGRESS record)
     * ============================================================ */
    @PostMapping("/{examId}/start")
    public ResponseEntity<ExamStartResponse> startExam(
            @PathVariable String examId,
            Principal principal
    ) {
        String email = principal.getName();
        ExamStartResponse response = studentExamService.startExam(examId, email);
        return ResponseEntity.ok(response);
    }

    /* ============================================================
     * 3️⃣ Fetch Exam Questions
     * ============================================================ */
    @GetMapping("/{examId}/questions")
    public ResponseEntity<List<ExamQuestionResponse>> getExamQuestions(
            @PathVariable String examId
    ) {
        List<ExamQuestionResponse> questions = studentExamService.getExamQuestions(examId);
        return ResponseEntity.ok(questions);
    }

    /* ============================================================
     * 4️⃣ Submit Exam Answers
     * ============================================================ */
    @PostMapping("/{examId}/submit")
    public ResponseEntity<ExamResultResponse> submitExam(
            @PathVariable String examId,
            @RequestBody ExamSubmitRequest request,
            Principal principal
    ) {
        String email = principal.getName();
        ExamResultResponse result = studentExamService.submitExam(examId, email, request);
        return ResponseEntity.ok(result);
    }

    /* ============================================================
     * 5️⃣ Get Exam Result (For Review Page)
     * ============================================================ */
    @GetMapping("/{examId}/result")
    public ResponseEntity<ExamResultResponse> getExamResult(
            @PathVariable String examId,
            Principal principal
    ) {
        String email = principal.getName();
        ExamResultResponse result = studentExamService.getExamResult(examId, email);
        return ResponseEntity.ok(result);
    }
}
