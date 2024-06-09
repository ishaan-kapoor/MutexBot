package com.sprinklr.msTeams.mutexBot.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.sprinklr.msTeams.mutexBot.utils.UserTimeEntry;

@Document(collection = "jenkins-resources")
public class Resource {
  @Id
  // private String _id;
  private String name;
  private boolean reserved;
  public String reservedBy;
  public LocalDateTime reservedTill;
  public List<UserTimeEntry> monitoredBy;

  public Resource(String name) {
    this.name = name;
    reserved = false;
    reservedBy = null;
    reservedTill = null;
    monitoredBy = new ArrayList<UserTimeEntry>();
  }

  public void reserve(String user, LocalDateTime till) {
    reserved = true;
    reservedBy = user;
    reservedTill = till;
  }

  public void release() {
    reserved = false;
  }

  public void clean_monitor_list() {
    LocalDateTime now = LocalDateTime.now();
    Iterator<UserTimeEntry> iterator = monitoredBy.iterator();
    while (iterator.hasNext()) {
      if (iterator.next().till.isBefore(now)) {
        iterator.remove();
      }
    }
  }

  public void stopMonitoring(String user) {
    Iterator<UserTimeEntry> iterator = monitoredBy.iterator();
    while (iterator.hasNext()) {
      if (iterator.next().user.equals(user)) {
        iterator.remove();
        break;
      }
    }
  }

  public void monitor(String user, LocalDateTime till) {
    boolean already_present = false;
    for (UserTimeEntry entry : monitoredBy) {
      if (entry.user.equals(user)) {
        already_present = true;
        if (entry.till.isAfter(till)) { break; }
        entry.till = till;
      }
    }
    if (!already_present) {
      UserTimeEntry entry = new UserTimeEntry(user, till);
      monitoredBy.add(entry);
    }
  }

  public String toString() {
    // return String.format("%s, %s, %s, %s, %s", name, reserved, reservedBy, reservedTill, monitoredBy);
    String message = name;
    if (isReserved()) {
      // System.out.println("res");
      message += String.format(" is reserved by %s till %s", reservedBy, reservedTill);
    }
    if (isMonitored()) {
      // System.out.println("mon");
      message += "\nMonitored by the following:";
      for (UserTimeEntry entry : monitoredBy) {
        message += String.format("\n\t%s", entry);
      }
    }
    return message;
  }

  public String getName() {
    return name;
  }

  public boolean isReserved() {
    // System.out.println("res called");
    // System.out.println(reserved);
    if (!reserved) { return false; }
    if (reservedTill == null) { return false; }
    if (reservedTill.isBefore(LocalDateTime.now())) { reserved = false; }
    return reserved;
  }

  public boolean isMonitored() {
    // System.out.println("mon called");
    if (monitoredBy == null) { return false; }
    return monitoredBy.size() > 0;
  }
}
