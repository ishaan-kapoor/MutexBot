package com.sprinklr.msTeams.mutexBot.repositories;

import com.sprinklr.msTeams.mutexBot.model.User;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * MongoDB repository interface for managing {@link User} entities.
 * Provides CRUD operations and custom query methods for interacting with the
 * "users" collection.
 */
@Repository
public interface UserRepository extends MongoRepository<User, String> {

  /**
   * Checks if a user with the specified ID exists.
   *
   * @param id The ID to check.
   * @return {@code true} if a user with the ID exists, otherwise {@code false}.
   */
  boolean existsById(String id);

  /**
   * Finds a user by their email address.
   *
   * @param email The email address of the user to find.
   * @return The {@link User} object associated with the email address, or
   *         {@code null} if not found.
   */
  User findByEmail(String email);

  /**
   * Retrieves a list of users who have admin privileges.
   *
   * @return A list of {@link User} objects where {@code admin} is {@code true}.
   */
  List<User> findByAdminTrue();
}
