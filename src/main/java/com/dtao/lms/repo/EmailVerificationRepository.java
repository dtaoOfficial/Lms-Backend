package com.dtao.lms.repo;

import com.dtao.lms.model.EmailVerificationToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailVerificationRepository extends MongoRepository<EmailVerificationToken, String> {

    Optional<EmailVerificationToken> findByEmailAndOtp(String email, String otp);

    Optional<EmailVerificationToken> findByEmail(String email);

    void deleteByEmail(String email);
}
