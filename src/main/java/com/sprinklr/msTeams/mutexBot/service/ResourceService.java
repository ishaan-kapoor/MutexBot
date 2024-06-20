package com.sprinklr.msTeams.mutexBot.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sprinklr.msTeams.mutexBot.model.Resource;
import com.sprinklr.msTeams.mutexBot.repositories.ResourceRepository;

@Service
public class ResourceService {

  @Autowired
  private ResourceRepository repo;
  private static Set<String> cache = Collections.synchronizedSet(new HashSet<>());
  private static AtomicInteger accessCount = new AtomicInteger(0);
  private static final int REFRESH_THRESHOLD = 500;

  @PostConstruct
  public void initializeCache() {
    refreshCache();
  }

  public synchronized void refreshCache() {
    // System.out.println("Cache Refresh");
    cache.clear();
    cache.addAll(repo.findAll().stream()
      .map(Resource::getName)
      .collect(Collectors.toList()));
    accessCount.set(0);
  }

  public void updateCache(String name, boolean add) {
    synchronized (cache) {
      if (add) {
        cache.add(name);
      } else {
        cache.remove(name);
      }
    }
  }

  private void incrementAccessCount() {
    if (accessCount.incrementAndGet() >= REFRESH_THRESHOLD) {
      refreshCache();
    }
  }

  public List<Resource> getAll() {
    incrementAccessCount();
    return repo.findAll();
  }

  public List<Resource> getReserved() {
    incrementAccessCount();
    return repo.findReservedResources(LocalDateTime.now());
  }

  public List<Resource> getAvailable() {
    incrementAccessCount();
    return repo.findAvailableResources(LocalDateTime.now());
  }

  public Resource find(String name) throws Exception {
    incrementAccessCount();
    Optional<Resource> resource = repo.findById(name);
    if (! resource.isPresent()) { return null; }
    return resource.get();
  }

  public void save(String resourceName) {  // for creating new resources
    repo.save(new Resource(resourceName));
    refreshCache();
  }

  public void save(Resource resource) {
    incrementAccessCount();
    repo.save(resource);
  }

  public boolean exists(String name) {
    // return repo.existsById(name);
    // System.out.println("Cache Hit");
    incrementAccessCount();
    return cache.contains(name);
  }

  public void delete(String name) {
    repo.deleteById(name);
    refreshCache();
  }

  public List<String> findByChartName(String chartName) {
    incrementAccessCount();
    return repo.findByIdStartingWith(chartName+"-").stream()
      .map(Resource::getName)
      .collect(Collectors.toList());
  }
}

