package com.sprinklr.msTeams.mutexBot.model;

import java.time.LocalDateTime;

/**
 * Represents an entry for user monitoring time.
 * Each entry includes the user and the monitoring end time.
 */
public class UserTimeEntry {
  public String user;
  public LocalDateTime till;

  public UserTimeEntry(String user, LocalDateTime till) {
    this.user = user;
    this.till = till;
  }

  public UserTimeEntry() { }

  public String toString() { return String.format("%s -> %s", user, till); }
}
