package com.dtao.lms.repo;

import com.dtao.lms.model.Video;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VideoRepository extends MongoRepository<Video, String> {
    List<Video> findByChapterIdOrderByOrderAsc(String chapterId);

    // Videos with no chapter but with valid courseId
    List<Video> findByCourseIdAndChapterIdIsNullOrderByOrderAsc(String courseId);
}
