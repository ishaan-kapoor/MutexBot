package com.sprinklr.msTeams.mutexBot.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.sprinklr.msTeams.mutexBot.Utils;

@Document(collection = "Monitor-Log")
public class MonitorLog {
  @Id
  private String _id;
  private String resource;
  private String user;
  public LocalDateTime start;
  public LocalDateTime end;

  public MonitorLog(String resource, String user, LocalDateTime start, LocalDateTime end) {
    this.resource = resource;
    this.user = user;
    this.start = start;
    this.end = end;
  }

  public String toString() {
    return String.format("%s monitored %s from %s till %s", user, resource, start.format(Utils.timeFormat), end.format(Utils.timeFormat));
  }
}

