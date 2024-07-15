package com.sprinklr.msTeams.mutexBot;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Pageable;

import com.sprinklr.msTeams.mutexBot.model.MonitorLog;
import com.sprinklr.msTeams.mutexBot.repositories.MonitorLogRepository;
import com.sprinklr.msTeams.mutexBot.service.MonitorLogService;

class MonitorLogServiceTest {

  @Mock
  private MonitorLogRepository monitorLogRepository;

  @InjectMocks
  private MonitorLogService monitorLogService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    reset(monitorLogRepository);
  }

  @Test
  void testGetLatest() {
    MonitorLog log = new MonitorLog("resource1", "user1", LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1));
    List<MonitorLog> logList = new ArrayList<>();
    logList.add(log);
    when(monitorLogRepository.getLatest(eq("resource1"), eq("user1"), any(Pageable.class))).thenReturn(logList);

    MonitorLog result = monitorLogService.getLatest("resource1", "user1");

    assertNotNull(result);
    assertEquals("resource1", result.getResource());
    assertEquals("user1", result.getUser());
  }

  @Test
  void testGetLatest_NoLog() {
    when(monitorLogRepository.getLatest(eq("resource1"), eq("user1"), any(Pageable.class))) .thenReturn(new ArrayList<>());

    MonitorLog result = monitorLogService.getLatest("resource1", "user1");

    assertNull(result);
  }

  @Test
  void testGetLogs() {
    MonitorLog log1 = new MonitorLog("resource1", "user1", LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1));
    MonitorLog log2 = new MonitorLog("resource1", "user2", LocalDateTime.now().minusHours(2), LocalDateTime.now().plusHours(2));
    List<MonitorLog> logList = new ArrayList<>();
    logList.add(log1);
    logList.add(log2);
    when(monitorLogRepository.getLogs("resource1", "user1")).thenReturn(logList);

    List<MonitorLog> result = monitorLogService.getLogs("resource1", "user1");

    assertEquals(2, result.size());
    assertEquals("resource1", result.get(0).getResource());
    assertEquals("user1", result.get(0).getUser());
  }

  @Test
  void testGetResourceLogs() {
    MonitorLog log1 = new MonitorLog("resource1", "user1", LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1));
    List<MonitorLog> logList = new ArrayList<>();
    logList.add(log1);
    when(monitorLogRepository.getResourceLogs("resource1")).thenReturn(logList);

    List<MonitorLog> result = monitorLogService.getResourceLogs("resource1");

    assertEquals(1, result.size());
    assertEquals("resource1", result.get(0).getResource());
  }

  @Test
  void testMonitor() {
    String resource = "resource1";
    String user = "user1";
    LocalDateTime end = LocalDateTime.now().plusHours(1);

    monitorLogService.monitor(resource, user, end);

    ArgumentCaptor<MonitorLog> captor = ArgumentCaptor.forClass(MonitorLog.class);
    verify(monitorLogRepository).save(captor.capture());
    MonitorLog savedLog = captor.getValue();

    assertEquals(resource, savedLog.getResource());
    assertEquals(user, savedLog.getUser());
    assertEquals(end, savedLog.getEndTime());
  }

  @Test
  void testStopMonitoring() {
    MonitorLog log = new MonitorLog("resource1", "user1", LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1));
    List<MonitorLog> logList = new ArrayList<>();
    logList.add(log);
    when(monitorLogRepository.getLatest(eq("resource1"), eq("user1"), any(Pageable.class))).thenReturn(logList);

    monitorLogService.stopMonitoring("resource1", "user1");

    ArgumentCaptor<MonitorLog> captor = ArgumentCaptor.forClass(MonitorLog.class);
    verify(monitorLogRepository).save(captor.capture());
    MonitorLog updatedLog = captor.getValue();

    assertEquals("resource1", updatedLog.getResource());
    assertEquals("user1", updatedLog.getUser());
    assertTrue(updatedLog.getEndTime().isBefore(LocalDateTime.now().plusSeconds(1)));
  }

  @Test
  void testGetUserLogs() {
    MonitorLog log1 = new MonitorLog("resource1", "user1", LocalDateTime.now().minusHours(1),
        LocalDateTime.now().plusHours(1));
    List<MonitorLog> logList = new ArrayList<>();
    logList.add(log1);
    when(monitorLogRepository.getUserLogs("user1")).thenReturn(logList);

    List<MonitorLog> result = monitorLogService.getUserLogs("user1");

    assertEquals(1, result.size());
    assertEquals("user1", result.get(0).getUser());
  }
}
