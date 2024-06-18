package com.sprinklr.msTeams.mutexBot.controler;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sprinklr.msTeams.mutexBot.model.MonitorLog;
import com.sprinklr.msTeams.mutexBot.model.ReservationLog;
import com.sprinklr.msTeams.mutexBot.model.User;
import com.sprinklr.msTeams.mutexBot.service.MonitorLogService;
import com.sprinklr.msTeams.mutexBot.service.ReservationLogService;
import com.sprinklr.msTeams.mutexBot.service.UserService;

@RestController
public class LogsController {

  private final MonitorLogService monitorLogService;
  private final ReservationLogService reservationLogService;
  private final UserService userService;

  @Autowired
  public LogsController(MonitorLogService monitorLogService, ReservationLogService reservationLogService, UserService userService) {
    this.monitorLogService = monitorLogService;
    this.reservationLogService = reservationLogService;
    this.userService = userService;
  }

  private String value(String userId, String resourceName, String perspective) {
    if (perspective == null) { perspective = new String("resource"); }
    perspective = perspective.toLowerCase();
    if (perspective.equals("resource")) {
      User user;
      try {
        user = userService.find(userId);
      } catch (Exception e) {
        e.printStackTrace();
        return "User";
      }
      return user.getName();
    }
    if (perspective.equals("user")) { return resourceName; }
    else { return "Invalid perspective"; }
  }

  @GetMapping("/logs")
  public String getUserLogs(@RequestParam(required = false) String resource, @RequestParam(required = false) String user, @RequestParam(required = false) String perspective) {
    String template;
    try {
      InputStream inputStream = getClass().getResourceAsStream("/report.html");
      if (inputStream == null) { return null; }
      template = IOUtils.toString(inputStream, StandardCharsets.UTF_8.toString());
    } catch (IOException e) {
      e.printStackTrace();
      return "Error while loading report template.<br>" + e;
    }
    if (template == null) { return "Report template not found"; }

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");// .withZone(ZoneOffset.UTC);
    List<MonitorLog> monitorLogs = monitorLogService.getLogs(resource, user);
    List<ReservationLog> reservationLogs = reservationLogService.getLogs(resource, user);

    String reservationRecords = reservationLogs.stream()
        .map(log -> String.format("{ title: 'Reserved - %s', start: '%s', end: '%s', classNames: ['reservation']}",
            value(log.user, log.resource, perspective),
            formatter.format(log.reservedAt.atOffset(ZoneOffset.UTC)),
            formatter.format(log.releasedAt.atOffset(ZoneOffset.UTC))))
        .collect(Collectors.joining(",\n      ", "[\n      ", "\n]"));

    String monitorRecords = monitorLogs.stream()
        .map(log -> String.format("{ title: 'Monitored - %s', start: '%s', end: '%s', classNames: ['monitor']}",
            value(log.user, log.resource, perspective),
            formatter.format(log.start.atOffset(ZoneOffset.UTC)),
            formatter.format(log.end.atOffset(ZoneOffset.UTC))))
        .collect(Collectors.joining(",\n      ", "[\n      ", "\n]"));

    String page = template;
    page = page.replaceFirst("\\[\\]", reservationRecords);
    page = page.replaceFirst("\\[\\]", monitorRecords);
    return page;
  }
}
