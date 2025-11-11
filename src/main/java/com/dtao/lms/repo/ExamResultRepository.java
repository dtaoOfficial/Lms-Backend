package com.dtao.lms.repo;

import com.dtao.lms.model.ExamResult;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ✅ ExamResultRepository
 * Handles persistence and custom queries for ExamResult documents.
 */
@Repository
public interface ExamResultRepository extends MongoRepository<ExamResult, String> {

    /**
     * Find all results for a specific student.
     */
    List<ExamResult> findByStudentEmail(String studentEmail);
    List<ExamResult> findByStudentEmailIgnoreCase(String email);

    /**
     * Find a student's result for a specific exam.
     */
    Optional<ExamResult> findByExamIdAndStudentEmail(String examId, String studentEmail);

    /**
     * Check if a student has already completed an exam.
     */
    boolean existsByExamIdAndStudentEmailAndStatus(String examId, String studentEmail, String status);

    /**
     * Find all completed results for an exam (for analytics later).
     */
    List<ExamResult> findByExamIdAndStatus(String examId, String status);
}
