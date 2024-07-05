package com.sprinklr.msTeams.mutexBot.repositories;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.sprinklr.msTeams.mutexBot.model.MonitorLog;

import java.util.List;

/**
 * MongoDB repository interface for managing {@link MonitorLog} entities.
 * Provides CRUD operations and custom query methods for interacting with the
 * "Monitor-Log" collection.
 */
@Repository
public interface MonitorLogRepository extends MongoRepository<MonitorLog, String> {

  /**
   * Retrieves the latest monitor logs for a specific resource and user.
   *
   * @param resource The resource identifier.
   * @param user     The user identifier.
   * @param pageable Pagination information.
   * @return A list of {@link MonitorLog} objects sorted by descending start time.
   */
  @Query(value = "{ 'resource': ?0, 'user': ?1 }", sort = "{ 'start': -1 }")
  List<MonitorLog> getLatest(String resource, String user, Pageable pageable);

  /**
   * Retrieves monitor logs for a specific resource and user.
   *
   * @param resource The resource identifier.
   * @param user     The user identifier.
   * @return A list of {@link MonitorLog} objects sorted by descending start time.
   */
  @Query(value = "{ 'resource': ?0, 'user': ?1 }", sort = "{ 'start': -1 }")
  List<MonitorLog> getLogs(String resource, String user);

  /**
   * Retrieves monitor logs for a specific resource.
   *
   * @param resource The resource identifier.
   * @return A list of {@link MonitorLog} objects sorted by descending start time.
   */
  @Query(value = "{ 'resource': ?0 }", sort = "{ 'start': -1 }")
  List<MonitorLog> getResourceLogs(String resource);

  /**
   * Retrieves monitor logs for a specific user.
   *
   * @param user The user identifier.
   * @return A list of {@link MonitorLog} objects sorted by descending start time.
   */
  @Query(value = "{ 'user': ?0 }", sort = "{ 'start': -1 }")
  List<MonitorLog> getUserLogs(String user);
}
