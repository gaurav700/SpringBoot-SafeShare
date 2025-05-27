
// Updated UserRepository.java - MongoDB Repository
package com.shareApp.Authentication.repositories;

import com.shareApp.Authentication.entities.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);

    // Case-insensitive email search
    @Query("{'email': {$regex: ?0, $options: 'i'}}")
    Optional<User> findByEmailIgnoreCase(String email);

    // Check if user exists by email
    boolean existsByEmail(String email);

    // Find enabled users only
    Optional<User> findByEmailAndEnabledTrue(String email);

    // Find verified users only
    Optional<User> findByEmailAndEmailVerifiedTrue(String email);
}