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

  private final MonitorLogRepository monitorLogRepository;

  @Autowired
  public MonitorLogService(MonitorLogRepository monitorLogRepository) {
    this.monitorLogRepository = monitorLogRepository;
  }

  public MonitorLog getLatest(String resource, String user) {
    Pageable pageable = PageRequest.of(0, 1);
    List<MonitorLog> log = monitorLogRepository.getLatest(resource, user, pageable);
    return log.isEmpty() ? null : log.get(0);
  }

  public List<MonitorLog> getLogs(String resource, String user) {
    return monitorLogRepository.getLogs(resource, user);
  }

  public List<MonitorLog> getResourceLogs(String resource) {
    return monitorLogRepository.getResourceLogs(resource);
  }

  public void monitor(String resource, String user, LocalDateTime end) {
    MonitorLog log = new MonitorLog(resource, user, LocalDateTime.now(), end);
    monitorLogRepository.save(log);
  }

  public void stopMonitoring(String resource, String user) {
    MonitorLog log = getLatest(resource, user);
    if (log == null) { return; }
    if (log.end.isBefore(LocalDateTime.now())) { return; }
    log.end = LocalDateTime.now();
    monitorLogRepository.save(log);
  }

  public List<MonitorLog> getUserLogs(String user) {
    return monitorLogRepository.getUserLogs(user);
  }
}
