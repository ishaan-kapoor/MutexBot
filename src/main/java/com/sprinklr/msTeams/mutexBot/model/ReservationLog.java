package com.sprinklr.msTeams.mutexBot.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.sprinklr.msTeams.mutexBot.Utils;

/**
 * Represents a reservation log entry for a resource by a user.
 * Each log entry contains details such as resource name, user ID,
 * start time, and end time.
 */
@Document(collection = "Reservation-Log")
public class ReservationLog {
  @Id
  private String _id;
  private String resource;
  private String user;
  private LocalDateTime reservedAt;
  private LocalDateTime releasedAt;

  public String getResource() { return resource; }
  public String getUser() { return user; }
  public LocalDateTime getStartTime() { return reservedAt; }
  public LocalDateTime getEndTime() { return releasedAt; }
  public void setEndTime(LocalDateTime releasedAt) { this.releasedAt = releasedAt; }

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
