package com.sprinklr.msTeams.mutexBot.repositories;

import com.sprinklr.msTeams.mutexBot.model.Resource;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ResourceRepository extends MongoRepository<Resource, String> {
  @Query("{ '_id': { $regex: '^?0' } }")
  List<Resource> findByIdStartingWith(String prefix);

  @Query("{ 'reserved': true, 'reservedTill': { $gt: ?0 } }")
  List<Resource> findReservedResources(LocalDateTime now);

  @Query("{ $or: [ { 'reserved': false }, { 'reserved': true, 'reservedTill': { $lt: ?0 } } ] }")
  List<Resource> findAvailableResources(LocalDateTime now);
  // boolean existsById(String id);
}

