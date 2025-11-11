package com.dtao.lms.repo;

import com.dtao.lms.model.Exam;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExamRepository extends MongoRepository<Exam, String> {

    /**
     * Find all published exams.
     * Useful for showing available exams to students.
     */
    List<Exam> findByIsPublishedTrue();

    /**
     * Find exams created by a specific admin.
     */
    List<Exam> findByCreatedBy(String adminId);

    /**
     * Find published exams within a specific date range (visible to students).
     */
    @Query("{ 'is_published': true, 'start_date': { $lte: ?0 }, 'end_date': { $gte: ?1 } }")
    List<Exam> findActiveExams(LocalDateTime currentStart, LocalDateTime currentEnd);

    /**
     * Find exam by name (for duplicate validation).
     */
    Optional<Exam> findByNameIgnoreCase(String name);
}
