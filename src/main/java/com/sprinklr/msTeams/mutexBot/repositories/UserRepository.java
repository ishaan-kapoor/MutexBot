package com.sprinklr.msTeams.mutexBot.repositories;

import com.sprinklr.msTeams.mutexBot.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
  boolean existsById(String id);
}
