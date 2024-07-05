package com.sprinklr.msTeams.mutexBot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.sprinklr.msTeams.mutexBot.model.MonitorLog;
import com.sprinklr.msTeams.mutexBot.repositories.MonitorLogRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service class for managing operations on {@link MonitorLog} entities.
 */
@Service
public class MonitorLogService {

  private final MonitorLogRepository repo;

  /**
   * Constructs a new {@code MonitorLogService} instance with the specified
   * repository.
   *
   * @param monitorLogRepository The repository for {@link MonitorLog} entities.
   */
  @Autowired
  public MonitorLogService(MonitorLogRepository monitorLogRepository) {
    this.repo = monitorLogRepository;
  }

  /**
   * Retrieves the latest monitor log for the specified resource and user.
   *
   * @param resource The resource identifier.
   * @param user     The user identifier.
   * @return The latest {@link MonitorLog} or {@code null} if none found.
   */
  public MonitorLog getLatest(String resource, String user) {
    Pageable pageable = PageRequest.of(0, 1);
    List<MonitorLog> log = repo.getLatest(resource, user, pageable);
    return log.isEmpty() ? null : log.get(0);
  }

  /**
   * Retrieves monitor logs filtered by resource and user.
   * If {@code user} is {@code null}, retrieves logs for the specified resource
   * only.
   * If {@code resource} is {@code null}, retrieves logs for the specified user
   * only.
   *
   * @param resource The resource identifier.
   * @param user     The user identifier.
   * @return A list of {@link MonitorLog} entities matching the criteria.
   */
  public List<MonitorLog> getLogs(String resource, String user) {
    if (user == null) { return getResourceLogs(resource); }
    if (resource == null) { return getUserLogs(user); }
    return repo.getLogs(resource, user);
  }

  /**
   * Retrieves monitor logs for the specified resource.
   *
   * @param resource The resource identifier.
   * @return A list of {@link MonitorLog} entities for the specified resource.
   */
  public List<MonitorLog> getResourceLogs(String resource) {
    return repo.getResourceLogs(resource);
  }

  /**
   * Initiates monitoring of a resource by a user until the specified end time.
   *
   * @param resource The resource identifier.
   * @param user     The user identifier.
   * @param end      The end time of the monitoring period.
   */
  public void monitor(String resource, String user, LocalDateTime end) {
    MonitorLog log = new MonitorLog(resource, user, LocalDateTime.now(), end);
    repo.save(log);
  }

  /**
   * Stops monitoring of a resource by a user, setting the end time to the current
   * time.
   *
   * @param resource The resource identifier.
   * @param user     The user identifier.
   */
  public void stopMonitoring(String resource, String user) {
    MonitorLog log = getLatest(resource, user);
    if (log == null) { return; }
    if (log.getEndTime().isBefore(LocalDateTime.now())) { return; }
    log.setEndTime(LocalDateTime.now());
    repo.save(log);
  }

  /**
   * Retrieves monitor logs for the specified user.
   *
   * @param user The user identifier.
   * @return A list of {@link MonitorLog} entities for the specified user.
   */
  public List<MonitorLog> getUserLogs(String user) {
    return repo.getUserLogs(user);
  }
}
