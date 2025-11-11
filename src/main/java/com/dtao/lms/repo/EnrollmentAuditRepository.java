package com.dtao.lms.repo;

import com.dtao.lms.model.EnrollmentAudit;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EnrollmentAuditRepository extends MongoRepository<EnrollmentAudit, String> {
}
