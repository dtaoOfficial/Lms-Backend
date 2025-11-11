package com.dtao.lms.repo;

import com.dtao.lms.model.AuthToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthTokenRepository extends MongoRepository<AuthToken, String> {
    Optional<AuthToken> findByToken(String token);
    Optional<AuthToken> findByTokenId(String tokenId);
}
