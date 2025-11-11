package com.dtao.lms.repo;

import com.dtao.lms.model.Session;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * SessionRepository
 *
 * Repository for managing Session documents in MongoDB.
 * Includes a few useful query helpers for analytics, cleanup, and filtering.
 */
@Repository
public interface SessionRepository extends MongoRepository<Session, String> {

    /**
     * Find a session by its public sessionId (UUID string).
     */
    Optional<Session> findBySessionId(String sessionId);

    /**
     * Find all sessions for a given user email.
     */
    List<Session> findByEmail(String email);

    /**
     * Find all currently active sessions for a given user.
     */
    List<Session> findByEmailAndIsActiveTrue(String email);

    /**
     * (Optional helper)
     * Find all active sessions that have not been touched since a given time — useful for expiring idle sessions.
     */
    @Query("{ 'isActive': true, 'lastSeenAt': { $lt: ?0 } }")
    List<Session> findActiveSessionsOlderThan(Instant before);

    /**
     * (Optional helper)
     * Find all inactive (ended) sessions for cleanup or audit history.
     */
    List<Session> findByIsActiveFalse();

    /**
     * (Optional helper)
     * Find all sessions that belong to a given email and started before a certain time (analytics / pruning).
     */
    @Query("{ 'email': ?0, 'loginAt': { $lt: ?1 } }")
    List<Session> findOldSessionsForEmail(String email, Instant before);
}
