package com.sprinklr.msTeams.mutexBot.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users")
public class User {
  @Id
  // private String _id;
  private String id;
  private String name;
  public User() {
    this("defaultID");
  }
  public User(String id) {
    this(id, "defaultName");
  }
  public User(String id, String name) {
    this.id = id;
    this.name = name;
  }
  public String getId() {
    return id;
  }
  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }
  public String toString() {
    return String.format("%s (%s)", name, id);
  }
}
