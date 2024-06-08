package com.sprinklr.msTeams.mutexBot.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sprinklr.msTeams.mutexBot.model.Resource;
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
    if (! user.isPresent()) {
      User new_user = new User(id);
      save(new_user);
      user = repo.findById(id);
      if (! user.isPresent()) {
        throw new Exception("Created a user, but couldn't fetch it.");
      }
    }
    return user.get();
  }

  public boolean exisits(String id) {
    return repo.existsById(id);
  }

  public void save(User user) {
    System.out.println("Saved");
    repo.save(user);
  }

}
