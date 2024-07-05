package com.sprinklr.msTeams.mutexBot.repositories;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.sprinklr.msTeams.mutexBot.model.ReservationLog;

import java.util.List;

/**
 * MongoDB repository interface for managing {@link ReservationLog} entities.
 * Provides CRUD operations and custom query methods for interacting with the
 * "Reservation-Log" collection.
 */
@Repository
public interface ReservationLogRepository extends MongoRepository<ReservationLog, String> {

  /**
   * Retrieves the latest reservation logs for a specific resource and user.
   *
   * @param resource The resource identifier.
   * @param user     The user identifier.
   * @param pageable Pagination information.
   * @return A list of {@link ReservationLog} objects sorted by descending
   *         reservation time.
   */
  @Query(value = "{ 'resource': ?0, 'user': ?1 }", sort = "{ 'reservedAt': -1 }")
  List<ReservationLog> getLatest(String resource, String user, Pageable pageable);

  /**
   * Retrieves reservation logs for a specific resource and user.
   *
   * @param resource The resource identifier.
   * @param user     The user identifier.
   * @return A list of {@link ReservationLog} objects sorted by descending
   *         reservation time.
   */
  @Query(value = "{ 'resource': ?0, 'user': ?1 }", sort = "{ 'reservedAt': -1 }")
  List<ReservationLog> getLogs(String resource, String user);

  /**
   * Retrieves reservation logs for a specific resource.
   *
   * @param resource The resource identifier.
   * @return A list of {@link ReservationLog} objects sorted by descending
   *         reservation time.
   */
  @Query(value = "{ 'resource': ?0 }", sort = "{ 'reservedAt': -1 }")
  List<ReservationLog> getResourceLogs(String resource);

  /**
   * Retrieves reservation logs for a specific user.
   *
   * @param user The user identifier.
   * @return A list of {@link ReservationLog} objects sorted by descending
   *         reservation time.
   */
  @Query(value = "{ 'user': ?0 }", sort = "{ 'reservedAt': -1 }")
  List<ReservationLog> getUserLogs(String user);
}
