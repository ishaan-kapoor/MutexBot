package com.sprinklr.msTeams.mutexBot.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.sprinklr.msTeams.mutexBot.model.ChartName;

@Repository
public interface ChartNameRepository extends MongoRepository<ChartName, String> {
}
