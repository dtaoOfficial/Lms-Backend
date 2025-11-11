package com.dtao.lms.repo;

import com.dtao.lms.model.Chapter;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChapterRepository extends MongoRepository<Chapter, String> {
    List<Chapter> findByCourseIdOrderByOrderAsc(String courseId);
}
