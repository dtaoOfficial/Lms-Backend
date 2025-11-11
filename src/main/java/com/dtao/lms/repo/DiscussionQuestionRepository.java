package com.dtao.lms.repo;

import com.dtao.lms.model.DiscussionQuestion;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface DiscussionQuestionRepository extends MongoRepository<DiscussionQuestion, String> {
    List<DiscussionQuestion> findByCourseIdOrderByCreatedAtDesc(String courseId);
}
