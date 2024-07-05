package com.sprinklr.msTeams.mutexBot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.sprinklr.msTeams.mutexBot.model.ReservationLog;
import com.sprinklr.msTeams.mutexBot.repositories.ReservationLogRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service class for managing operations on {@link ReservationLog} entities.
 */
@Service
public class ReservationLogService {

  private final ReservationLogRepository repo;

  /**
   * Constructs a new {@code ReservationLogService} instance with the specified
   * repository.
   *
   * @param reservationLogRepository The repository for {@link ReservationLog}
   *                                 entities.
   */
  @Autowired
  public ReservationLogService(ReservationLogRepository reservationLogRepository) {
    this.repo = reservationLogRepository;
  }

  /**
   * Retrieves the latest reservation log for the specified resource and user.
   *
   * @param resource The resource identifier.
   * @param user     The user identifier.
   * @return The latest {@link ReservationLog} or {@code null} if none found.
   */
  public ReservationLog getLatest(String resource, String user) {
    Pageable pageable = PageRequest.of(0, 1);
    List<ReservationLog> log = repo.getLatest(resource, user, pageable);
    return log.isEmpty() ? null : log.get(0);
  }

  /**
   * Retrieves reservation logs filtered by resource and user.
   * If {@code user} is {@code null}, retrieves logs for the specified resource
   * only.
   * If {@code resource} is {@code null}, retrieves logs for the specified user
   * only.
   *
   * @param resource The resource identifier.
   * @param user     The user identifier.
   * @return A list of {@link ReservationLog} entities matching the criteria.
   */
  public List<ReservationLog> getLogs(String resource, String user) {
    if (user == null) { return getResourceLogs(resource); }
    if (resource == null) { return getUserLogs(user); }
    return repo.getLogs(resource, user);
  }

  /**
   * Retrieves reservation logs for the specified resource.
   *
   * @param resource The resource identifier.
   * @return A list of {@link ReservationLog} entities for the specified resource.
   */
  public List<ReservationLog> getResourceLogs(String resource) {
    return repo.getResourceLogs(resource);
  }

  /**
   * Initiates reservation of a resource by a user until the specified releasedAt
   * time.
   *
   * @param resource   The resource identifier.
   * @param user       The user identifier.
   * @param releasedAt The time until which the resource is reserved.
   */
  public void reserve(String resource, String user, LocalDateTime releasedAt) {
    ReservationLog log = new ReservationLog(resource, user, LocalDateTime.now(), releasedAt);
    repo.save(log);
  }

  /**
   * Releases reservation of a resource by a user, setting the end time to the
   * current time.
   *
   * @param resource The resource identifier.
   * @param user     The user identifier.
   */
  public void release(String resource, String user) {
    ReservationLog log = getLatest(resource, user);
    if (log == null) { return; }
    log.setEndTime(LocalDateTime.now());
    repo.save(log);
  }

  /**
   * Retrieves reservation logs for the specified user.
   *
   * @param user The user identifier.
   * @return A list of {@link ReservationLog} entities for the specified user.
   */
  public List<ReservationLog> getUserLogs(String user) {
    return repo.getUserLogs(user);
  }
}
