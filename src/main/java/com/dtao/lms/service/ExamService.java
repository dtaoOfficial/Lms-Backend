package com.dtao.lms.service;

import com.dtao.lms.model.Exam;
import com.dtao.lms.model.User;
import com.dtao.lms.repo.ExamRepository;
import com.dtao.lms.repo.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ExamService {

    private static final Logger log = LoggerFactory.getLogger(ExamService.class);

    private final ExamRepository examRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired(required = false)
    private EmailNotificationService emailNotificationService;

    @Autowired
    public ExamService(ExamRepository examRepository) {
        this.examRepository = examRepository;
    }

    /**
     * Create a new exam (validates duplicates and notifies students)
     */
    public Exam createExam(Exam exam) {
        Optional<Exam> existingExam = examRepository.findByNameIgnoreCase(exam.getName());
        if (existingExam.isPresent()) {
            throw new RuntimeException("Exam with name '" + exam.getName() + "' already exists.");
        }

        Exam saved = examRepository.save(exam);

        // ✅ Notify all verified students about new exam
        try {
            if (emailNotificationService != null && userRepository != null) {
                List<User> students = userRepository.findByRoleIgnoreCase("STUDENT")
                        .stream()
                        .filter(User::isVerified)
                        .toList();

                if (!students.isEmpty()) {
                    emailNotificationService.sendNewExamNotification(saved, students);
                    log.info("📢 Sent new exam notification to {} students for exam {}", students.size(), saved.getName());
                }
            }
        } catch (Exception e) {
            log.error("Failed to send new exam notification: {}", e.getMessage(), e);
        }

        return saved;
    }

    /**
     * Get all exams (admin use)
     */
    public List<Exam> getAllExams() {
        return examRepository.findAll();
    }

    /**
     * Get all published exams (student use)
     */
    public List<Exam> getPublishedExams() {
        return examRepository.findByIsPublishedTrue();
    }

    /**
     * Get currently active exams (within date range)
     */
    public List<Exam> getActiveExams() {
        LocalDateTime now = LocalDateTime.now();
        return examRepository.findActiveExams(now, now);
    }

    /**
     * Find exam by ID
     */
    public Exam getExamById(String id) {
        return examRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Exam not found with ID: " + id));
    }

    /**
     * Update existing exam (basic details only)
     */
    public Exam updateExam(String id, Exam updatedExam) {
        Exam existingExam = getExamById(id);

        existingExam.setName(updatedExam.getName());
        existingExam.setType(updatedExam.getType());
        existingExam.setLanguage(updatedExam.getLanguage());
        existingExam.setStartDate(updatedExam.getStartDate());
        existingExam.setEndDate(updatedExam.getEndDate());
        existingExam.setDuration(updatedExam.getDuration());

        return examRepository.save(existingExam);
    }

    /**
     * Toggle publish/unpublish
     */
    public Exam togglePublish(String id, boolean publish) {
        Exam exam = getExamById(id);
        exam.setPublished(publish);
        return examRepository.save(exam);
    }

    /**
     * Delete exam by ID
     */
    public void deleteExam(String id) {
        Exam exam = getExamById(id);
        examRepository.delete(exam);
    }
}
