package com.sprinklr.msTeams.mutexBot.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.sprinklr.msTeams.mutexBot.model.ChartName;

/**
 * MongoDB repository interface for managing {@link ChartName} entities.
 * Provides CRUD operations and query methods for interacting with the
 * "ChartNames" collection.
 */
@Repository
public interface ChartNameRepository extends MongoRepository<ChartName, String> {
}
