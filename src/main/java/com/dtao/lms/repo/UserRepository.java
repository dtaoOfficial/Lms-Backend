package com.dtao.lms.repo;

import com.dtao.lms.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);

    List<User> findByRole(String role); // ✅ existing
    List<User> findByRoleIgnoreCase(String role);
    long countByRole(String role);

    // ✅ NEW METHODS for dashboard stats
    long countByActiveTrue(); // count active users
    Optional<User> findByEmailIgnoreCase(String email);
    long countByVerifiedTrue(); // count verified users
}
