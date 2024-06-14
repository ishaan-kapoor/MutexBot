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

  private final ReservationLogRepository reservationLogRepository;

  @Autowired
  public ReservationLogService(ReservationLogRepository reservationLogRepository) {
    this.reservationLogRepository = reservationLogRepository;
  }

  public ReservationLog getLatest(String resource, String user) {
    Pageable pageable = PageRequest.of(0, 1);
    List<ReservationLog> log = reservationLogRepository.getLatest(resource, user, pageable);
    return log.isEmpty() ? null : log.get(0);
  }

  public List<ReservationLog> getLogs(String resource, String user) {
    return reservationLogRepository.getLogs(resource, user);
  }

  public List<ReservationLog> getResourceLogs(String resource) {
    return reservationLogRepository.getResourceLogs(resource);
  }

  public void reserve(String resource, String user, LocalDateTime releasedAt) {
    ReservationLog log = new ReservationLog(resource, user, LocalDateTime.now(), releasedAt);
    reservationLogRepository.save(log);
  }

  public void release(String resource, String user) {
    ReservationLog log = getLatest(resource, user);
    if (log == null) { return; }
    log.releasedAt = LocalDateTime.now();
    reservationLogRepository.save(log);
  }

  public List<ReservationLog> getUserLogs(String user) {
    return reservationLogRepository.getUserLogs(user);
  }
}
