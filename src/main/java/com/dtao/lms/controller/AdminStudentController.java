package com.dtao.lms.controller;

import com.dtao.lms.model.Course;
import com.dtao.lms.model.ExamResult;
import com.dtao.lms.model.User;
import com.dtao.lms.repo.*;
import com.dtao.lms.service.LeaderboardService;
import com.dtao.lms.service.ProgressService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/student")
@CrossOrigin(
        origins = "${APP_CORS_ALLOWED_ORIGINS:http://localhost:3000}",
        allowCredentials = "true",
        allowedHeaders = "*"
)
@PreAuthorize("hasRole('ADMIN')")
public class AdminStudentController {

    @Autowired private UserRepository userRepository;
    @Autowired private ExamResultRepository examResultRepository;
    @Autowired private ExamRepository examRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private ProgressService progressService;
    @Autowired private LeaderboardService leaderboardService;

    @Value("${APP_CORS_ALLOWED_ORIGINS:http://localhost:3000}")
    private String allowedOrigins;

    /**
     * 🧠 Fetch student profile + rank + XP
     */
    @GetMapping("/{email}")
    public ResponseEntity<?> getStudentProfile(@PathVariable String email) {
        Optional<User> opt = userRepository.findByEmailIgnoreCase(email);
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Student not found"));
        }
        User user = opt.get();

        int xp = leaderboardService.getXpForStudent(email);
        int rank = leaderboardService.generateGlobalLeaderboard().getEntries().stream()
                .filter(e -> e.getEmail().equalsIgnoreCase(email))
                .map(e -> e.getRank())
                .findFirst()
                .orElse(0);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", user.getName());
        map.put("email", user.getEmail());
        map.put("department", user.getDepartment());
        map.put("phone", user.getPhone());
        map.put("createdAt", user.getCreatedAt());
        map.put("xp", xp);
        map.put("rank", rank);

        return ResponseEntity.ok(map);
    }

    /**
     * 🧾 Get all exams for a student (with title, total marks, percentage, etc.)
     */
    @GetMapping("/{email}/exams")
    public ResponseEntity<?> getStudentExams(@PathVariable String email) {
        List<ExamResult> results = examResultRepository.findByStudentEmailIgnoreCase(email);
        if (results.isEmpty()) return ResponseEntity.ok(List.of());

        List<Map<String, Object>> enriched = results.stream().map(r -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("examId", r.getExamId());
            map.put("studentEmail", r.getStudentEmail());
            map.put("studentName", r.getStudentName());
            map.put("score", r.getScore());
            map.put("percentage", r.getPercentage());
            map.put("status", r.getStatus());

            int totalMarks = 0;
            try {
                var examOpt = examRepository.findById(r.getExamId());
                if (examOpt.isPresent()) {
                    var exam = examOpt.get();
                    totalMarks = (exam.getQuestions() != null)
                            ? exam.getQuestions().size()
                            : 0;
                    map.put("title", exam.getName());
                } else {
                    map.put("title", "Unknown Exam");
                }
            } catch (Exception ex) {
                map.put("title", "Unknown Exam");
            }

            map.put("totalMarks", totalMarks);
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(enriched);
    }

    /**
     * 🎓 Get all approved courses and progress for a student
     */
    @GetMapping("/{email}/courses")
    public ResponseEntity<?> getStudentCourses(@PathVariable String email) {
        var enrollments = enrollmentRepository.findByEmailAndStatus(email, "APPROVED");
        List<Map<String, Object>> list = new ArrayList<>();

        for (var e : enrollments) {
            var courseOpt = courseRepository.findById(e.getCourseId());
            if (courseOpt.isEmpty()) continue;
            Course c = courseOpt.get();

            double progress = progressService.getCourseCompletionPercent(email, c.getId());
            long completedVideos = progressService.getTotalCompletedVideos(email);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("title", c.getTitle());
            row.put("progressPercent", progress);
            row.put("completedVideos", completedVideos);
            list.add(row);
        }

        return ResponseEntity.ok(list);
    }

    // ✅ Optional: log CORS origins on startup (for Render logs)
    @PostConstruct
    public void logAllowedOrigins() {
        System.out.println("🌍 [AdminStudentController] Allowed origins: " + allowedOrigins);
    }
}
