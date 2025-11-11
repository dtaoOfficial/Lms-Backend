package com.dtao.lms.repo;

import com.dtao.lms.model.Enrollment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends MongoRepository<Enrollment, String> {

    // 🟢 Base queries
    Optional<Enrollment> findByCourseIdAndEmail(String courseId, String email);
    List<Enrollment> findByEmail(String email);
    List<Enrollment> findByStatus(String status);
    List<Enrollment> findByCourseId(String courseId);
    List<Enrollment> findByEmailAndStatus(String email, String status);
    long countByStatus(String status);
    List<Enrollment> findByCreatedAtAfter(Instant date);

    // ✅ Dashboard Queries — using correct field name "email"

    // 🔹 Count total enrolled courses for student
    @Query(value = "{ 'email' : { $regex: ?0, $options: 'i' } }", count = true)
    long countByEmailRegex(String email);

    // 🔹 Get all enrollments by email (case-insensitive)
    @Query(value = "{ 'email' : { $regex: ?0, $options: 'i' } }")
    List<Enrollment> findAllByEmailRegex(String email);

    // 🔹 Recently enrolled courses (for dashboard preview)
    @Query(value = "{ 'email' : ?0 }", fields = "{ 'courseId' : 1, 'createdAt' : 1 }", sort = "{ 'createdAt' : -1 }")
    List<Enrollment> findRecentEnrollments(@Param("email") String email);
}
