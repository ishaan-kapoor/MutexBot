package com.sprinklr.msTeams.mutexBot.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.microsoft.bot.schema.ChannelAccount;
import com.microsoft.bot.schema.teams.TeamsChannelAccount;

@Document(collection = "users")
public class User {
  @Id
  private String id;
  private String name;
  private String email;
  private boolean admin;
  public static String defaultEmail = "defaultEmail";
  public static String defaultName = "defaultName";
  public static String defaultId = "defaultID";

  public User() {
    this(defaultId);
  }

  public User(String id) {
    this(id, defaultName, defaultEmail);
  }

  public User(ChannelAccount user) {
    this(user.getId(), user.getName(), defaultEmail);
  }

  public User(TeamsChannelAccount user) {
    this(user.getId(), user.getName(), user.getEmail());
  }

  public User(String id, String name, String email) {
    this.id = id;
    this.name = name;
    this.email = email;
    this.admin = false;
  }

  public boolean isAdmin() { return admin; }

  public String getId() { return id; }

  public String getName() { return name; }

  public String getEmail() { return email; }

  public void setName(String name) { this.name = name; }

  public void setEmail(String email) { this.email = email; }

  public String toString() { return String.format("%s (%s)", name, id); }

  public void makeAdmin() { admin = true; }

  public void dismissAdmin() { admin = false; }
}
