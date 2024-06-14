package com.sprinklr.msTeams.mutexBot.repositories;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.sprinklr.msTeams.mutexBot.model.MonitorLog;

import java.util.List;

@Repository
public interface MonitorLogRepository extends MongoRepository<MonitorLog, String> {

  @Query(value = "{ 'resource': ?0, 'user': ?1 }", sort = "{ 'start': -1 }")
  List<MonitorLog> getLatest(String resource, String user, Pageable pageable);

  @Query(value = "{ 'resource': ?0, 'user': ?1 }", sort = "{ 'start': -1 }")
  List<MonitorLog> getLogs(String resource, String user);

  @Query(value = "{ 'resource': ?0 }", sort = "{ 'start': -1 }")
  List<MonitorLog> getResourceLogs(String resource);

  @Query(value = "{ 'user': ?0 }", sort = "{ 'start': -1 }")
  List<MonitorLog> getUserLogs(String user);
}
