package com.sprinklr.msTeams.mutexBot.utils;

import java.time.LocalDateTime;

public class UserTimeEntry {
  public String user;
  public LocalDateTime till;
  public UserTimeEntry(String user, LocalDateTime till) {
    this.user = user;
    this.till = till;
  }
  public UserTimeEntry() {}
  public String toString() {
    return String.format("%s -> %s", user, till);
  }
}

