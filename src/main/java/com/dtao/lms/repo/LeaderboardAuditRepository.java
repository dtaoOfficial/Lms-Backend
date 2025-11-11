package com.dtao.lms.repo;

import com.dtao.lms.model.LeaderboardAudit;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeaderboardAuditRepository extends MongoRepository<LeaderboardAudit, String> {
    List<LeaderboardAudit> findAllByOrderByResetAtDesc();
}
