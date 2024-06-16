package com.sprinklr.msTeams.mutexBot.repositories;

import com.sprinklr.msTeams.mutexBot.model.Resource;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ResourceRepository extends MongoRepository<Resource, String> {
  @Query("{ '_id': { $regex: '^?0' } }")
  List<Resource> findByIdStartingWith(String prefix);
  // boolean existsById(String id);
}

