package com.sprinklr.msTeams.mutexBot.controler;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sprinklr.msTeams.mutexBot.model.MonitorLog;
import com.sprinklr.msTeams.mutexBot.model.Resource;
import com.sprinklr.msTeams.mutexBot.service.MonitorLogService;
import com.sprinklr.msTeams.mutexBot.service.ResourceService;

@RestController
@RequestMapping("/logs")
public class MonitorLogsController {

  private final MonitorLogService monitorLogService;

  @Autowired
  public MonitorLogsController(MonitorLogService monitorLogService) {
    this.monitorLogService = monitorLogService;
  }

  @GetMapping("/resource/")
  public String getResourceLogs() {
    List<MonitorLog> logs = monitorLogService.getResourceLogs("intuition-ms-qa6-tier2");
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    return logs.stream()
    .map(log -> String.format("{\n  title: '%s',\n  start: '%s',\n  end: '%s'\n}",
      log.user,
      formatter.format(log.start.atOffset(ZoneOffset.UTC)),
      formatter.format(log.end.atOffset(ZoneOffset.UTC))))
    .collect(Collectors.joining(",\n      ", "[\n      ", "\n]"));
  }
}
