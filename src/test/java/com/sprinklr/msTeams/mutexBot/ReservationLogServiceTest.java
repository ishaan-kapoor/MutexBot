package com.sprinklr.msTeams.mutexBot;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.sprinklr.msTeams.mutexBot.model.ReservationLog;
import com.sprinklr.msTeams.mutexBot.repositories.ReservationLogRepository;
import com.sprinklr.msTeams.mutexBot.service.ReservationLogService;

class ReservationLogServiceTest {

  @Mock
  private ReservationLogRepository reservationLogRepository;

  @InjectMocks
  private ReservationLogService reservationLogService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    reset(reservationLogRepository);
  }

  @Test
  void testGetLatest() {
    ReservationLog log = new ReservationLog("Resource1", "User1", LocalDateTime.now(), LocalDateTime.now().plusHours(1));
    Pageable pageable = PageRequest.of(0, 1);
    when(reservationLogRepository.getLatest("Resource1", "User1", pageable)).thenReturn(Collections.singletonList(log));

    ReservationLog latestLog = reservationLogService.getLatest("Resource1", "User1");

    assertNotNull(latestLog);
    assertEquals(log, latestLog);
    verify(reservationLogRepository, times(1)).getLatest("Resource1", "User1", pageable);
  }

  @Test
  void testGetLatest_NoLog() {
    Pageable pageable = PageRequest.of(0, 1);
    when(reservationLogRepository.getLatest("Resource1", "User1", pageable)).thenReturn(Collections.emptyList());

    ReservationLog latestLog = reservationLogService.getLatest("Resource1", "User1");

    assertNull(latestLog);
    verify(reservationLogRepository, times(1)).getLatest("Resource1", "User1", pageable);
  }

  @Test
  void testGetLogs() {
    ReservationLog log = new ReservationLog("Resource1", "User1", LocalDateTime.now(), LocalDateTime.now().plusHours(1));
    when(reservationLogRepository.getLogs("Resource1", "User1")).thenReturn(Collections.singletonList(log));

    List<ReservationLog> logs = reservationLogService.getLogs("Resource1", "User1");

    assertNotNull(logs);
    assertEquals(1, logs.size());
    assertEquals(log, logs.get(0));
    verify(reservationLogRepository, times(1)).getLogs("Resource1", "User1");
  }

  @Test
  void testGetResourceLogs() {
    ReservationLog log = new ReservationLog("Resource1", "User1", LocalDateTime.now(), LocalDateTime.now().plusHours(1));
    when(reservationLogRepository.getResourceLogs("Resource1")).thenReturn(Collections.singletonList(log));

    List<ReservationLog> logs = reservationLogService.getResourceLogs("Resource1");

    assertNotNull(logs);
    assertEquals(1, logs.size());
    assertEquals(log, logs.get(0));
    verify(reservationLogRepository, times(1)).getResourceLogs("Resource1");
  }

  @Test
  void testReserve() {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime releaseTime = now.plusHours(1);
    ReservationLog log = new ReservationLog("Resource1", "User1", now, releaseTime);

    reservationLogService.reserve("Resource1", "User1", releaseTime);

    verify(reservationLogRepository, times(1)).save(any(ReservationLog.class));
  }

  @Test
  void testRelease() {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime releaseTime = now.plusHours(1);
    ReservationLog log = new ReservationLog("Resource1", "User1", now, releaseTime);
    when(reservationLogRepository.getLatest("Resource1", "User1", PageRequest.of(0, 1)))
        .thenReturn(Collections.singletonList(log));

    reservationLogService.release("Resource1", "User1");

    assertNotNull(log.getEndTime());
    verify(reservationLogRepository, times(1)).save(log);
  }

  @Test
  void testRelease_NoLog() {
    when(reservationLogRepository.getLatest("Resource1", "User1", PageRequest.of(0, 1)))
        .thenReturn(Collections.emptyList());

    reservationLogService.release("Resource1", "User1");

    verify(reservationLogRepository, never()).save(any(ReservationLog.class));
  }

  @Test
  void testGetUserLogs() {
    ReservationLog log = new ReservationLog("Resource1", "User1", LocalDateTime.now(),
        LocalDateTime.now().plusHours(1));
    when(reservationLogRepository.getUserLogs("User1")).thenReturn(Collections.singletonList(log));

    List<ReservationLog> logs = reservationLogService.getUserLogs("User1");

    assertNotNull(logs);
    assertEquals(1, logs.size());
    assertEquals(log, logs.get(0));
    verify(reservationLogRepository, times(1)).getUserLogs("User1");
  }
}
