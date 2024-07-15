package com.sprinklr.msTeams.mutexBot;

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

/**
 * Class to present reservation and monitor logs in a calendar like UI.
 */
@RestController
public class LogCalendar {

  private final MonitorLogService monitorLogService;
  private final ReservationLogService reservationLogService;
  private final UserService userService;

  /**
   * Constructs a LogCalendar with the specified services.
   *
   * @param monitorLogService     the service to handle monitor logs
   * @param reservationLogService the service to handle reservation logs
   * @param userService           the service to handle user information
   */
  @Autowired
  public LogCalendar(
      MonitorLogService monitorLogService,
      ReservationLogService reservationLogService,
      UserService userService) {
    this.monitorLogService = monitorLogService;
    this.reservationLogService = reservationLogService;
    this.userService = userService;
  }

  /**
   * Retrieves the user or resource name based on the provided perspective.
   *
   * @param userId       the ID of the user
   * @param resourceName the name of the resource
   * @param perspective  the perspective for the logs ("resource" or "user")
   * @return the name of the user or resource based on the perspective
   */
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
    } else if (perspective.equals("user")) {
      return resourceName;
    } else {
      return "Invalid perspective";
    }
  }

  /**
   * Endpoint to fetch user/resource logs.
   * 
   * @param resource    the name of the resource (optional)
   * @param user        the ID of the user (optional)
   * @param perspective the perspective of the logs ("resource" or "user")
   *                    (optional)
   * @return the HTML formatted logs
   */
  @GetMapping("/logs")
  public String getUserLogs(
      @RequestParam(required = false) String resource,
      @RequestParam(required = false) String user,
      @RequestParam(required = false) String perspective) {
    String template;
    try {
      InputStream inputStream = getClass().getResourceAsStream("/report.html");
      if (inputStream == null) {
        return null;
      }
      template = IOUtils.toString(inputStream, StandardCharsets.UTF_8.toString());
    } catch (IOException e) {
      e.printStackTrace();
      return "Error while loading report template.<br>" + e;
    }
    if (template == null) {
      return "Report template not found";
    }

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");// .withZone(ZoneOffset.UTC);
    List<MonitorLog> monitorLogs = monitorLogService.getLogs(resource, user);
    List<ReservationLog> reservationLogs = reservationLogService.getLogs(resource, user);

    String reservationRecords = reservationLogs.stream()
        .map(log -> String.format(
            "{ title: 'Reserved - %s', start: '%s', end: '%s', classNames: ['reservation']}",
            value(log.getUser(), log.getResource(), perspective),
            formatter.format(log.getStartTime().atOffset(ZoneOffset.UTC)),
            formatter.format(log.getEndTime().atOffset(ZoneOffset.UTC))))
        .collect(Collectors.joining(",\n      ", "[\n      ", "\n]"));

    String monitorRecords = monitorLogs.stream()
        .map(log -> String.format(
            "{ title: 'Monitored - %s', start: '%s', end: '%s', classNames: ['monitor']}",
            value(log.getUser(), log.getResource(), perspective),
            formatter.format(log.getStartTime().atOffset(ZoneOffset.UTC)),
            formatter.format(log.getEndTime().atOffset(ZoneOffset.UTC))))
        .collect(Collectors.joining(",\n      ", "[\n      ", "\n]"));

    String page = template;
    page = page.replaceFirst("\\[\\]", reservationRecords);
    page = page.replaceFirst("\\[\\]", monitorRecords);
    return page;
  }
}
