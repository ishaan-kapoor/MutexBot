package com.sprinklr.msTeams.mutexBot.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.sprinklr.msTeams.mutexBot.Utils;

@Document(collection = "Reservation-Log")
public class ReservationLog {
  @Id
  private String _id;
  private String resource;
  private String user;
  public LocalDateTime reservedAt;
  public LocalDateTime releasedAt;

  public ReservationLog(String resource, String user, LocalDateTime reservedAt, LocalDateTime releasedAt) {
    this.resource = resource;
    this.user = user;
    this.reservedAt = reservedAt;
    this.releasedAt = releasedAt;
  }

  public String toString() {
    return String.format("%s reserved %s from %s till %s", user, resource, reservedAt.format(Utils.timeFormat), releasedAt.format(Utils.timeFormat));
  }
}

