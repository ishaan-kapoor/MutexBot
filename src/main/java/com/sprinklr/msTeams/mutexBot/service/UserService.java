package com.sprinklr.msTeams.mutexBot.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.microsoft.bot.schema.ChannelAccount;
import com.sprinklr.msTeams.mutexBot.Utils;
import com.sprinklr.msTeams.mutexBot.model.User;
import com.sprinklr.msTeams.mutexBot.repositories.UserRepository;

/**
 * Service class for managing operations on {@link User} entities.
 */
@Service
public class UserService {
  @Autowired
  private UserRepository repo;

  /**
   * Retrieves all users from the repository.
   *
   * @return A list of all {@link User} entities.
   */
  public List<User> getAll() {
    return repo.findAll();
  }

  /**
   * Finds a user by their ID. If the user does not exist, creates a new user and
   * saves it.
   *
   * @param id The ID of the user to find.
   * @return The {@link User} entity corresponding to the ID.
   * @throws Exception If a user is created but cannot be fetched afterwards.
   */
  public User find(String id) throws Exception {
    Optional<User> user = repo.findById(id);
    if (!user.isPresent()) {
      User new_user = new User(id);
      save(new_user);
      user = repo.findById(id);
      if (!user.isPresent()) {
        throw new Exception("Created a user, but couldn't fetch it.");
      }
    }
    return user.get();
  }

  /**
   * Finds a user by their email address.
   *
   * @param email The email address of the user to find.
   * @return The {@link User} entity corresponding to the email address, or
   *         {@code null} if not found.
   */
  public User findByEmail(String email) {
    return repo.findByEmail(email);
  }

  /**
   * Checks if a user exists in the repository based on their ID.
   *
   * @param user The {@link ChannelAccount} representing the user to check.
   * @return {@code true} if the user exists, {@code false} otherwise.
   */
  public boolean exists(ChannelAccount user) {
    return exists(user.getId());
  }

  /**
   * Checks if a user exists in the repository based on their ID.
   *
   * @param id The ID of the user to check.
   * @return {@code true} if the user exists, {@code false} otherwise.
   */
  public boolean exists(String id) {
    // if (id.equals(User.defaultId)) { return false; }
    if (!repo.existsById(id)) { return false; }
    User user;
    try {
      user = find(id);
    } catch (Exception e) {
      System.out.println("Error while finding user: " + e.getMessage());
      e.printStackTrace();
      return false;
    }

    if (user.getEmail().equals(User.defaultEmail)) { return false; }
    if (user.getName().equals(User.defaultName)) { return false; }
    return true;
  }

  /**
   * Saves a user entity to the repository.
   *
   * @param user The {@link User} entity to save.
   */
  public void save(User user) {
    repo.save(user);
  }

  /**
   * Lists all users who are administrators.
   *
   * @return A string listing all admin users, formatted as HTML hyperlinks.
   */
  public String listAdmins() {
    return repo.findByAdminTrue().stream()
        .map(user -> Utils.user2hyperlink(user))
        .collect(Collectors.joining("<br>"));
  }

}
