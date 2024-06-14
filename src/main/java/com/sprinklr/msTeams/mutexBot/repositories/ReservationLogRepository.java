package com.sprinklr.msTeams.mutexBot.repositories;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.sprinklr.msTeams.mutexBot.model.ReservationLog;

import java.util.List;

@Repository
public interface ReservationLogRepository extends MongoRepository<ReservationLog, String> {

  @Query(value = "{ 'resource': ?0, 'user': ?1 }", sort = "{ 'reservedAt': -1 }")
  List<ReservationLog> getLatest(String resource, String user, Pageable pageable);

  @Query(value = "{ 'resource': ?0, 'user': ?1 }", sort = "{ 'reservedAt': -1 }")
  List<ReservationLog> getLogs(String resource, String user);

  @Query(value = "{ 'resource': ?0 }", sort = "{ 'reservedAt': -1 }")
  List<ReservationLog> getResourceLogs(String resource);

  @Query(value = "{ 'user': ?0 }", sort = "{ 'reservedAt': -1 }")
  List<ReservationLog> getUserLogs(String user);
}
