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

@Service
public class UserService {
  @Autowired
  private UserRepository repo;

  public List<User> getAll() {
    return repo.findAll();
  }

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

  public User findByEmail(String email) {
    return repo.findByEmail(email);
  }

  public boolean exists(ChannelAccount user) {
    return exists(user.getId());
  }

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

  public void save(User user) {
    repo.save(user);
  }

  public String listAdmins() {
    return repo.findByAdminTrue().stream()
        .map(user -> Utils.user2hyperlink(user))
        .collect(Collectors.joining("<br>"));
  }

}
