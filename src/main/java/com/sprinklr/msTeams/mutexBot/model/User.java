package com.sprinklr.msTeams.mutexBot.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.microsoft.bot.schema.ChannelAccount;
import com.microsoft.bot.schema.teams.TeamsChannelAccount;

/**
 * Represents a user in the system.
 * Users have an ID, name, email, and administrative privileges.
 */
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

  /**
   * Constructor with ID parameter. Initializes a user with the given ID,
   * default name, and default email.
   * 
   * @param id The ID of the user.
   */
  public User(String id) {
    this(id, defaultName, defaultEmail);
  }

  /**
   * Constructor using a ChannelAccount. Initializes a user with the ID and name
   * from the ChannelAccount,
   * and default email.
   * 
   * @param user The ChannelAccount containing user ID and name.
   */
  public User(ChannelAccount user) {
    this(user.getId(), user.getName(), defaultEmail);
  }

  /**
   * Constructor using a TeamsChannelAccount. Initializes a user with the ID,
   * name, and email from the TeamsChannelAccount.
   * 
   * @param user The TeamsChannelAccount containing user ID, name, and email.
   */
  public User(TeamsChannelAccount user) {
    this(user.getId(), user.getName(), user.getEmail());
  }

  /**
   * Constructor with full parameters. Initializes a user with the given ID, name,
   * and email.
   * By default, the user is not an admin.
   * 
   * @param id    The ID of the user.
   * @param name  The name of the user.
   * @param email The email of the user.
   */
  public User(String id, String name, String email) {
    this.id = id;
    this.name = name;
    this.email = email;
    this.admin = false;
  }

  /**
   * Checks if the user has administrative privileges.
   * 
   * @return true if the user is an admin, false otherwise.
   */
  public boolean isAdmin() { return admin; }

  public String getId() { return id; }

  public String getName() { return name; }

  public String getEmail() { return email; }

  public void setName(String name) { this.name = name; }

  public void setEmail(String email) { this.email = email; }

  public String toString() { return String.format("%s (%s)", name, id); }

  /**
   * Grants administrative privileges to the user.
   */
  public void makeAdmin() { admin = true; }

  /**
   * Revokes administrative privileges from the user.
   */
  public void dismissAdmin() { admin = false; }
}
