package com.dtao.lms.repo;

import com.dtao.lms.model.LikeRecord;
import com.dtao.lms.model.LikeType;
import com.dtao.lms.model.TargetType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LikeRepository extends MongoRepository<LikeRecord, String> {

    Optional<LikeRecord> findByTargetTypeAndTargetIdAndEmail(TargetType targetType, String targetId, String email);
    long countByTargetTypeAndTargetIdAndType(TargetType targetType, String targetId, LikeType type);
    List<LikeRecord> findByTargetTypeAndTargetId(TargetType targetType, String targetId);
    void deleteByTargetTypeAndTargetIdAndEmail(TargetType targetType, String targetId, String email);

    // --- NEW METHODS for leaderboard ---
    /** Count total likes given by a specific student (useful for leaderboard). */
    long countByEmailAndType(String email, LikeType type);

    /** Fetch all like records by a student. */
    List<LikeRecord> findByEmail(String email);
}
