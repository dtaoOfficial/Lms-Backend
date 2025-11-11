package com.dtao.lms.repo;

import com.dtao.lms.model.Course;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseRepository extends MongoRepository<Course, String> {

    // ✅ NEW METHOD for counting courses by creator or status
    @Query(value = "{ 'createdBy' : ?0 }", count = true)
    long countByTeacherEmail(String email);

    @Query(value = "{ 'status' : ?0 }", count = true)
    long countByStatus(String status);
}
