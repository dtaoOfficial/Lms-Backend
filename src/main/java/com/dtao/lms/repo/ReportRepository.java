package com.dtao.lms.repo;

import com.dtao.lms.model.Report;
import com.dtao.lms.model.TargetType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ReportRepository extends MongoRepository<Report, String> {

    List<Report> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    List<Report> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(TargetType targetType, String targetId);
    long countByStatus(String status);

    // ✅ NEW: generic find all (any status)
    List<Report> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
