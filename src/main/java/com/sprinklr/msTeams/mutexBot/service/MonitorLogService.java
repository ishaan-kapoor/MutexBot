package com.sprinklr.msTeams.mutexBot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.sprinklr.msTeams.mutexBot.model.MonitorLog;
import com.sprinklr.msTeams.mutexBot.repositories.MonitorLogRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MonitorLogService {

  private final MonitorLogRepository repo;

  @Autowired
  public MonitorLogService(MonitorLogRepository monitorLogRepository) {
    this.repo = monitorLogRepository;
  }

  public MonitorLog getLatest(String resource, String user) {
    Pageable pageable = PageRequest.of(0, 1);
    List<MonitorLog> log = repo.getLatest(resource, user, pageable);
    return log.isEmpty() ? null : log.get(0);
  }

  public List<MonitorLog> getLogs(String resource, String user) {
    if (user == null) { return getResourceLogs(resource); }
    if (resource == null) { return getUserLogs(user); }
    return repo.getLogs(resource, user);
  }

  public List<MonitorLog> getResourceLogs(String resource) {
    return repo.getResourceLogs(resource);
  }

  public void monitor(String resource, String user, LocalDateTime end) {
    MonitorLog log = new MonitorLog(resource, user, LocalDateTime.now(), end);
    repo.save(log);
  }

  public void stopMonitoring(String resource, String user) {
    MonitorLog log = getLatest(resource, user);
    if (log == null) { return; }
    if (log.getEndTime().isBefore(LocalDateTime.now())) { return; }
    log.setEndTime(LocalDateTime.now());
    repo.save(log);
  }

  public List<MonitorLog> getUserLogs(String user) {
    return repo.getUserLogs(user);
  }
}
