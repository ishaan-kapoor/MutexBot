package com.sprinklr.msTeams.mutexBot.repositories;

import com.sprinklr.msTeams.mutexBot.model.Resource;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * MongoDB repository interface for managing {@link Resource} entities.
 * Provides CRUD operations and custom query methods for interacting with the
 * "resources" collection.
 */
@Repository
public interface ResourceRepository extends MongoRepository<Resource, String> {

  /**
   * Retrieves resources whose ID starts with a specified prefix.
   *
   * @param prefix The prefix to search for in resource IDs.
   * @return A list of {@link Resource} objects matching the prefix.
   */
  @Query("{ '_id': { $regex: '^?0' } }")
  List<Resource> findByIdStartingWith(String prefix);

  /**
   * Retrieves resources that are currently reserved and reserved until a
   * specified time.
   *
   * @param now The current date and time to check against reserved resources.
   * @return A list of {@link Resource} objects that are reserved until the
   *         specified time.
   */
  @Query("{ 'reserved': true, 'reservedTill': { $gt: ?0 } }")
  List<Resource> findReservedResources(LocalDateTime now);

  /**
   * Retrieves resources that are currently available (not reserved or reserved
   * until a past time).
   *
   * @param now The current date and time to check against resource availability.
   * @return A list of {@link Resource} objects that are currently available.
   */
  @Query("{ $or: [ { 'reserved': false }, { 'reserved': true, 'reservedTill': { $lt: ?0 } } ] }")
  List<Resource> findAvailableResources(LocalDateTime now);

  // boolean existsById(String id);
}
