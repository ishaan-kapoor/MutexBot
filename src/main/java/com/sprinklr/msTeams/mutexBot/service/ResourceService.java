package com.sprinklr.msTeams.mutexBot.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sprinklr.msTeams.mutexBot.model.Resource;
import com.sprinklr.msTeams.mutexBot.repositories.ResourceRepository;

@Service
public class ResourceService {

  @Autowired
  private ResourceRepository repo;

  public List<Resource> getAll() {
    return repo.findAll();
  }

  public Resource find(String name) throws Exception {
    Optional<Resource> resource = repo.findById(name);
    if (! resource.isPresent()) { return null; }
    return resource.get();
  }

  public void save(Resource resource) {
    repo.save(resource);
  }

  public boolean exists(String name) {
    return repo.existsById(name);
  }

  public void delete(String name) {
    repo.deleteById(name);
  }

}

