package com.dtao.lms.repo;

import com.dtao.lms.model.Comment;
import com.dtao.lms.model.TargetType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CommentRepository extends MongoRepository<Comment, String> {
    List<Comment> findByTargetTypeAndTargetIdAndDeletedOrderByCreatedAtDesc(TargetType targetType, String targetId, boolean deleted, Pageable pageable);
    long countByTargetTypeAndTargetIdAndDeleted(TargetType targetType, String targetId, boolean deleted);
}
