package com.sprinklr.msTeams.mutexBot.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Represents a resource that can be reserved and monitored by users.
 * Tracks reservation status, reservation duration, and monitoring details.
 */
@Document(collection = "jenkins-resources")
public class Resource {
  @Id
  // private String _id;
  private String name;
  private boolean reserved;
  private String reservedBy;
  private LocalDateTime reservedTill;
  private List<UserTimeEntry> monitoredBy;

  /** Maximum allocation time for resource reservation in minutes. */
  public int maxAllocationTime = 24 * 60;

  public String getReservedBy() { return reservedBy; }
  public LocalDateTime getReservedTill() { return reservedTill; }
  public List<UserTimeEntry> getMonitoredBy() { return monitoredBy; }

  /**
   * Constructs a new Resource with the specified name.
   * 
   * @param name The name or identifier of the resource.
   */
  public Resource(String name) {
    this.name = name;
    reserved = false;
    reservedBy = null;
    reservedTill = null;
    monitoredBy = new ArrayList<UserTimeEntry>();
  }

  /**
   * Reserves the resource for a specific user until a given time.
   * 
   * @param user The ID of the user reserving the resource.
   * @param till The LocalDateTime until when the resource is reserved.
   */
  public void reserve(String user, LocalDateTime till) {
    reserved = true;
    reservedBy = user;
    reservedTill = till;
  }

  /**
   * Releases the resource, making it available for reservation.
   */
  public void release() {
    reserved = false;
  }

  /**
   * Cleans up the list of users monitoring the resource by removing entries where
   * monitoring has expired.
   */
  public void clean_monitor_list() {
    LocalDateTime now = LocalDateTime.now();
    Iterator<UserTimeEntry> iterator = monitoredBy.iterator();
    while (iterator.hasNext()) {
      if (iterator.next().till.isBefore(now)) {
        iterator.remove();
      }
    }
  }

  /**
   * Stops monitoring the resource for a specific user.
   * 
   * @param user The ID of the user to stop monitoring.
   * @return true if the user was successfully removed from monitoring, false
   *         otherwise.
   */
  public boolean stopMonitoring(String user) {
    Iterator<UserTimeEntry> iterator = monitoredBy.iterator();
    while (iterator.hasNext()) {
      if (iterator.next().user.equals(user)) {
        iterator.remove();
        return true;
      }
    }
    return false;
  }

  /**
   * Starts monitoring the resource for a specific user until a given time.
   * If the user is already monitoring the resource, updates the monitoring
   * duration.
   * 
   * @param user The ID of the user monitoring the resource.
   * @param till The LocalDateTime until when the user will monitor the resource.
   */
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

  /**
   * Returns a string representation of the Resource object.
   * 
   * @return A string describing the resource, including its name, reservation
   *         status, and monitoring details.
   */
  public String toString() {
    String message = name;
    if (isReserved()) {
      message += String.format(" is reserved by %s till %s", reservedBy, reservedTill);
    }
    if (isMonitored()) {
      message += "\nMonitored by the following:";
      for (UserTimeEntry entry : monitoredBy) {
        message += String.format("\n\t%s", entry);
      }
    }
    return message;
  }

  public String getName() { return name; }

  /**
   * Checks if the resource is currently reserved.
   * 
   * @return true if the resource is reserved, false otherwise.
   */
  public boolean isReserved() {
    if (!reserved) { return false; }
    if (reservedTill == null) { return false; }
    if (reservedTill.isBefore(LocalDateTime.now())) { reserved = false; }
    return reserved;
  }

  /**
   * Checks if the resource is currently being monitored by any user.
   * 
   * @return true if the resource is being monitored, false otherwise.
   */
  public boolean isMonitored() {
    if (monitoredBy == null) { return false; }
    return monitoredBy.size() > 0;
  }
}
