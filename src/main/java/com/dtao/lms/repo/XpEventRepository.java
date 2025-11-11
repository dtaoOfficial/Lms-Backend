package com.dtao.lms.repo;

import com.dtao.lms.model.XpEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface XpEventRepository extends MongoRepository<XpEvent, String> {

    List<XpEvent> findByEmailOrderByCreatedAtDesc(String email);

    @Query(value = "{ 'email': ?0, 'type': ?1, 'videoId': ?2 }", count = true)
    long countByEmailAndTypeAndVideoId(String email, String type, String videoId);

    @Query(value = "{ 'email': ?0, 'type': ?1, 'questionId': ?2 }", count = true)
    long countByEmailAndTypeAndQuestionId(String email, String type, String questionId);

    @Query(value = "{ 'email': ?0, 'type': ?1, 'courseId': ?2 }", count = true)
    long countByEmailAndTypeAndCourseId(String email, String type, String courseId);
}
