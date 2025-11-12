// StudentExamService.java
package com.dtao.lms.service;

import com.dtao.lms.dto.*;
import com.dtao.lms.model.Exam;
import com.dtao.lms.model.ExamResult;
import com.dtao.lms.model.Question;
import com.dtao.lms.model.User;
import com.dtao.lms.repo.ExamRepository;
import com.dtao.lms.repo.ExamResultRepository;
import com.dtao.lms.repo.UserRepository;
import com.dtao.lms.utils.ExamEvaluatorUtil;
import org.springframework.scheduling.annotation.Async; // <- ADDED
import org.springframework.beans.factory.annotation.Autowired; // <- ADDED
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ✅ StudentExamService
 * Handles all student exam operations — listing, starting, submitting, and fetching results.
 */
@Service
public class StudentExamService {

    private final ExamRepository examRepository;
    private final ExamResultRepository examResultRepository;
    private final UserRepository userRepository;
    private final LeaderboardService leaderboardService; // <- ADDED

    // ✅ Manual constructor replacing Lombok @RequiredArgsConstructor
    @Autowired
    public StudentExamService(ExamRepository examRepository,
                              ExamResultRepository examResultRepository,
                              UserRepository userRepository,
                              LeaderboardService leaderboardService) { // <- UPDATED
        this.examRepository = examRepository;
        this.examResultRepository = examResultRepository;
        this.userRepository = userRepository;
        this.leaderboardService = leaderboardService;
    }

    /* ============================================================
     * 1️⃣ Get Available Exams
     * ============================================================ */
    public List<ExamResponse> getAvailableExams() {
        Instant now = Instant.now();
        return examRepository.findAll().stream()
                .filter(exam ->
                        exam.isPublished()
                                && exam.getStartDate() != null
                                && exam.getEndDate() != null
                                && now.isAfter(exam.getStartDate().atZone(java.time.ZoneId.systemDefault()).toInstant())
                                && now.isBefore(exam.getEndDate().atZone(java.time.ZoneId.systemDefault()).toInstant())
                )
                .map(this::toExamResponse)
                .collect(Collectors.toList());
    }

    /* ============================================================
     * 2️⃣ Get Exams for Student
     * ============================================================ */
    public List<ExamResponse> getAvailableExamsForStudent(String studentEmail) {
        Instant now = Instant.now();

        return examRepository.findAll().stream()
                .filter(exam ->
                        exam.isPublished()
                                && exam.getStartDate() != null
                                && exam.getEndDate() != null
                                && now.isAfter(exam.getStartDate().atZone(java.time.ZoneId.systemDefault()).toInstant())
                                && now.isBefore(exam.getEndDate().atZone(java.time.ZoneId.systemDefault()).toInstant())
                )
                .map(exam -> {
                    String status = examResultRepository
                            .findByExamIdAndStudentEmail(exam.getId(), studentEmail)
                            .map(ExamResult::getStatus)
                            .orElse("NOT_STARTED");

                    ExamResponse dto = toExamResponse(exam);
                    dto.setStudentStatus(status);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /* ============================================================
     * 3️⃣ Start Exam
     * ============================================================ */
    public ExamStartResponse startExam(String examId, String studentEmail) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found"));

        Instant now = Instant.now();
        Instant start = exam.getStartDate().atZone(java.time.ZoneId.systemDefault()).toInstant();
        Instant end = exam.getEndDate().atZone(java.time.ZoneId.systemDefault()).toInstant();

        if (now.isBefore(start) || now.isAfter(end)) {
            throw new RuntimeException("Exam not currently active");
        }

        boolean alreadyTaken = examResultRepository
                .existsByExamIdAndStudentEmailAndStatus(examId, studentEmail, "COMPLETED");
        if (alreadyTaken) {
            throw new RuntimeException("You already completed this exam");
        }

        String studentName = userRepository.findByEmailIgnoreCase(studentEmail)
                .map(User::getName)
                .filter(name -> name != null && !name.isBlank())
                .orElseGet(() -> {
                    String prefix = studentEmail.split("@")[0];
                    prefix = prefix.replace('.', ' ').replace('_', ' ');
                    return Character.toUpperCase(prefix.charAt(0)) + prefix.substring(1);
                });

        ExamResult result = examResultRepository
                .findByExamIdAndStudentEmail(examId, studentEmail)
                .orElseGet(() -> {
                    ExamResult newResult = new ExamResult();
                    newResult.setExamId(exam.getId());
                    newResult.setExamName(exam.getName());
                    newResult.setStudentEmail(studentEmail);
                    newResult.setStudentName(studentName);
                    newResult.setStatus("IN_PROGRESS");
                    newResult.setStartTime(Instant.now());
                    return newResult;
                });

        if (result.getStudentName() == null || result.getStudentName().isBlank()) {
            result.setStudentName(studentName);
        }

        examResultRepository.save(result);

        return ExamStartResponse.builder()
                .examId(exam.getId())
                .examName(exam.getName())
                .durationMinutes(exam.getDuration())
                .totalQuestions(exam.getQuestions().size())
                .startTime(result.getStartTime())
                .build();
    }

    /* ============================================================
     * 4️⃣ Get Questions
     * ============================================================ */
    public List<ExamQuestionResponse> getExamQuestions(String examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found"));

        List<Question> questions = new ArrayList<>(exam.getQuestions());
        Collections.shuffle(questions);

        return questions.stream()
                .map(q -> ExamQuestionResponse.builder()
                        .questionId(q.getId())
                        .question(q.getQuestion())
                        .optionA(q.getOptionA())
                        .optionB(q.getOptionB())
                        .optionC(q.getOptionC())
                        .optionD(q.getOptionD())
                        .build())
                .collect(Collectors.toList());
    }

    /* ============================================================
     * 5️⃣ Submit Exam (optimized: quick save + async evaluation)
     * ============================================================ */
    public ExamResultResponse submitExam(String examId, String studentEmail, ExamSubmitRequest request) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found"));

        ExamResult result = examResultRepository
                .findByExamIdAndStudentEmail(examId, studentEmail)
                .orElseThrow(() -> new RuntimeException("Exam session not found"));

        if ("COMPLETED".equals(result.getStatus())) {
            throw new RuntimeException("Exam already submitted");
        }

        // ✅ Save minimal data instantly and mark as evaluating
        result.setStatus("EVALUATING");
        result.setSubmittedAt(Instant.now());
        examResultRepository.save(result);

        // ⚡ Fire-and-forget heavy evaluation + leaderboard update
        evaluateAndFinalizeExamAsync(exam, result, request);

        // 🧠 Immediate response for frontend
        return ExamResultResponse.builder()
                .examId(exam.getId())
                .examName(exam.getName())
                .studentEmail(studentEmail)
                .status("PROCESSING")
                .performanceMessage("Your submission is being evaluated. Results will appear soon.")
                .build();
    }

    /* ============================================================
     * 5.1 Async worker: evaluate answers, save result, update leaderboard
     * ============================================================ */
    @Async("taskExecutor") // runs on taskExecutor threadpool
    public void evaluateAndFinalizeExamAsync(Exam exam, ExamResult result, ExamSubmitRequest request) {
        try {
            ExamEvaluatorUtil.EvaluationResult eval =
                    ExamEvaluatorUtil.evaluateAnswers(exam.getQuestions(), request.getAnswers());

            result.setAnswers(eval.getAnswerRecords());
            result.setCorrectCount(eval.getCorrectCount());
            result.setWrongCount(eval.getWrongCount());
            result.setTotalQuestions(eval.getTotalQuestions());
            result.setPercentage(eval.getPercentage());
            result.setScore(eval.getCorrectCount());
            result.setStatus("COMPLETED");
            result.setDurationSeconds(
                    result.getStartTime() != null
                            ? Instant.now().getEpochSecond() - result.getStartTime().getEpochSecond()
                            : 0
            );
            result.setSubmittedAt(Instant.now());

            examResultRepository.save(result);

            // 🏆 Async leaderboard update (assumes leaderboardService has this method)
            try {
                leaderboardService.updateLeaderboardAsync(result);
            } catch (Exception le) {
                // swallow leaderboard errors but log
                System.err.println("⚠️ Leaderboard update failed for " + result.getStudentEmail() + ": " + le.getMessage());
            }

            System.out.println("✅ [Async] Exam evaluated for " + result.getStudentEmail());

        } catch (Exception e) {
            System.err.println("💥 Error evaluating exam for " + result.getStudentEmail() + ": " + e.getMessage());
        }
    }

    /* ============================================================
     * 6️⃣ Fetch Result
     * ============================================================ */
    public ExamResultResponse getExamResult(String examId, String studentEmail) {
        ExamResult result = examResultRepository
                .findByExamIdAndStudentEmail(examId, studentEmail)
                .orElseThrow(() -> new RuntimeException("No result found for this exam"));

        if (result.getStudentName() == null || result.getStudentName().isBlank()) {
            String fetchedName = userRepository.findByEmailIgnoreCase(studentEmail)
                    .map(User::getName)
                    .orElse("Unknown Student");
            result.setStudentName(fetchedName);
            examResultRepository.save(result);
        }

        return toResultResponse(result);
    }

    /* ============================================================
     * 🧩 Helpers
     * ============================================================ */
    private ExamResponse toExamResponse(Exam exam) {
        return ExamResponse.builder()
                .id(exam.getId())
                .name(exam.getName())
                .type(exam.getType())
                .language(exam.getLanguage())
                .startDate(exam.getStartDate())
                .endDate(exam.getEndDate())
                .duration(exam.getDuration())
                .published(exam.isPublished())
                .build();
    }

    private ExamResultResponse toResultResponse(ExamResult result) {
        double percentage = result.getPercentage();

        return ExamResultResponse.builder()
                .examId(result.getExamId())
                .examName(result.getExamName())
                .studentEmail(result.getStudentEmail())
                .studentName(result.getStudentName())
                .totalQuestions(result.getTotalQuestions())
                .correctCount(result.getCorrectCount())
                .wrongCount(result.getWrongCount())
                .percentage(percentage)
                .score(result.getScore())
                .answers(result.getAnswers())
                .status(result.getStatus())
                .submittedAt(result.getSubmittedAt())
                .performanceMessage(getPerformanceMessage(percentage))
                .grade(getGrade(percentage))
                .build();
    }

    private String getPerformanceMessage(double percentage) {
        if (percentage >= 90) return "🔥 Excellent performance!";
        if (percentage >= 75) return "👏 Great job!";
        if (percentage >= 50) return "🙂 Good effort!";
        if (percentage > 0) return "💪 Don’t give up — try again!";
        return "❌ No correct answers. Study and retry!";
    }

    private String getGrade(double percentage) {
        if (percentage >= 90) return "A";
        if (percentage >= 75) return "B";
        if (percentage >= 50) return "C";
        if (percentage >= 35) return "D";
        return "F";
    }
}
