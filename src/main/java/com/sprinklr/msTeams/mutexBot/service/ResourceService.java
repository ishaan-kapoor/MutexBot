package com.sprinklr.msTeams.mutexBot.service;

import java.time.LocalDateTime;
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

/**
 * Service class for managing operations on {@link Resource} entities.
 */
@Service
public class ResourceService {

  @Autowired
  private ResourceRepository repo;
  private static Set<String> cache = Collections.synchronizedSet(new HashSet<>());
  private static AtomicInteger accessCount = new AtomicInteger(0);
  private static final int REFRESH_THRESHOLD = 500;

  /**
   * Initializes the cache of resource names upon bean creation.
   */
  @PostConstruct
  public void initializeCache() {
    refreshCache();
  }

  /**
   * Refreshes the cache of resource names from the repository.
   */
  public synchronized void refreshCache() {
    cache.clear();
    cache.addAll(repo.findAll().stream()
            .map(Resource::getName)
            .collect(Collectors.toList()));
    accessCount.set(0);
  }

  /**
   * Updates the cache with a new resource name or removes an existing one.
   *
   * @param name The name of the resource to update in the cache.
   * @param add  {@code true} to add the resource name, {@code false} to remove
   *             it.
   */
  public void updateCache(String name, boolean add) {
    synchronized (cache) {
      if (add) {
        cache.add(name);
      } else {
        cache.remove(name);
      }
    }
  }

  /**
   * Increments the access count and refreshes the cache if it exceeds the
   * threshold.
   */
  private void incrementAccessCount() {
    if (accessCount.incrementAndGet() >= REFRESH_THRESHOLD) {
      refreshCache();
    }
  }

  /**
   * Retrieves all resources from the repository.
   *
   * @return A list of all {@link Resource} entities.
   */
  public List<Resource> getAll() {
    incrementAccessCount();
    return repo.findAll();
  }

  /**
   * Retrieves the names of all resources from the repository.
   *
   * @return A list of all resource names.
   */
  public List<String> getAllNames() {
    incrementAccessCount();
    return repo.findAll().stream()
        .map(Resource::getName)
        .collect(Collectors.toList());
  }

  /**
   * Retrieves all reserved resources from the repository.
   *
   * @return A list of reserved {@link Resource} entities.
   */
  public List<Resource> getReserved() {
    incrementAccessCount();
    return repo.findReservedResources(LocalDateTime.now());
  }

  /**
   * Retrieves all available resources from the repository.
   *
   * @return A list of available {@link Resource} entities.
   */
  public List<Resource> getAvailable() {
    incrementAccessCount();
    return repo.findAvailableResources(LocalDateTime.now());
  }

  /**
   * Finds a resource by its name.
   *
   * @param name The name of the resource to find.
   * @return The {@link Resource} entity corresponding to the name, or
   *         {@code null} if not found.
   * @throws Exception If the resource is created but cannot be fetched
   *                   afterwards.
   */
  public Resource find(String name) throws Exception {
    incrementAccessCount();
    Optional<Resource> resource = repo.findById(name);
    if (!resource.isPresent()) { return null; }
    return resource.get();
  }

  /**
   * Saves a new resource with the given name.
   *
   * @param resourceName The name of the resource to save.
   */
  public void save(String resourceName) { // for creating new resources
    repo.save(new Resource(resourceName));
    refreshCache();
  }

  /**
   * Saves a resource entity to the repository.
   *
   * @param resource The {@link Resource} entity to save.
   */
  public void save(Resource resource) {
    incrementAccessCount();
    repo.save(resource);
  }

  /**
   * Checks if a resource exists in the cache by its name.
   *
   * @param name The name of the resource to check.
   * @return {@code true} if the resource exists in the cache, {@code false}
   *         otherwise.
   */
  public boolean exists(String name) {
    incrementAccessCount();
    return cache.contains(name);
  }

  /**
   * Deletes a resource by its name.
   *
   * @param name The name of the resource to delete.
   */
  public void delete(String name) {
    repo.deleteById(name);
    refreshCache();
  }

  /**
   * Finds all resources whose IDs start with the given chart name prefix.
   *
   * @param chartName The prefix of the resource IDs to search for.
   * @return A list of resource names matching the prefix.
   */
  public List<String> findByChartName(String chartName) {
    incrementAccessCount();
    return repo.findByIdStartingWith(chartName + "-").stream()
        .map(Resource::getName)
        .collect(Collectors.toList());
  }
}
