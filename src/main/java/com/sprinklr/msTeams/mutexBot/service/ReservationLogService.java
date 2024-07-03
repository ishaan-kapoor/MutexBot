package com.sprinklr.msTeams.mutexBot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.sprinklr.msTeams.mutexBot.model.ReservationLog;
import com.sprinklr.msTeams.mutexBot.repositories.ReservationLogRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReservationLogService {

  private final ReservationLogRepository repo;

  @Autowired
  public ReservationLogService(ReservationLogRepository reservationLogRepository) {
    this.repo = reservationLogRepository;
  }

  public ReservationLog getLatest(String resource, String user) {
    Pageable pageable = PageRequest.of(0, 1);
    List<ReservationLog> log = repo.getLatest(resource, user, pageable);
    return log.isEmpty() ? null : log.get(0);
  }

  public List<ReservationLog> getLogs(String resource, String user) {
    if (user == null) { return getResourceLogs(resource); }
    if (resource == null) { return getUserLogs(user); }
    return repo.getLogs(resource, user);
  }

  public List<ReservationLog> getResourceLogs(String resource) {
    return repo.getResourceLogs(resource);
  }

  public void reserve(String resource, String user, LocalDateTime releasedAt) {
    ReservationLog log = new ReservationLog(resource, user, LocalDateTime.now(), releasedAt);
    repo.save(log);
  }

  public void release(String resource, String user) {
    ReservationLog log = getLatest(resource, user);
    if (log == null) { return; }
    log.setEndTime(LocalDateTime.now());
    repo.save(log);
  }

  public List<ReservationLog> getUserLogs(String user) {
    return repo.getUserLogs(user);
  }
}
