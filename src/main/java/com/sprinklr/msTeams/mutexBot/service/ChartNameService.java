package com.sprinklr.msTeams.mutexBot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sprinklr.msTeams.mutexBot.model.ChartName;
import com.sprinklr.msTeams.mutexBot.repositories.ChartNameRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Service class for managing operations on {@link ChartName} entities.
 */
@Service
public class ChartNameService {

  private final ChartNameRepository repo;

  /**
   * Constructs a new {@code ChartNameService} instance with the specified
   * repository.
   *
   * @param chartNamesRepository The repository for {@link ChartName} entities.
   */
  @Autowired
  public ChartNameService(ChartNameRepository chartNamesRepository) {
    repo = chartNamesRepository;
  }

  /**
   * Saves a new chart name.
   *
   * @param chartName The name of the chart to save.
   */
  public void save(String chartName) {
    repo.save(new ChartName(chartName));
  }

  /**
   * Saves a {@link ChartName} entity.
   *
   * @param chartName The {@link ChartName} entity to save.
   */
  public void save(ChartName chartName) {
    repo.save(chartName);
  }

  /**
   * Deletes a chart name by its name.
   *
   * @param chartName The name of the chart to delete.
   */
  public void delete(String chartName) {
    repo.deleteById(chartName);
  }

  /**
   * Deletes a {@link ChartName} entity.
   *
   * @param chartName The {@link ChartName} entity to delete.
   */
  public void delete(ChartName chartName) {
    repo.deleteById(chartName.getName());
  }

  /**
   * Checks if a chart name exists.
   *
   * @param name The name of the chart to check.
   * @return {@code true} if a chart with the name exists, otherwise
   *         {@code false}.
   */
  public boolean exists(String name) {
    return repo.existsById(name);
  }

  /**
   * Retrieves a list of all chart names.
   *
   * @return A list of chart names.
   */
  public List<String> getAll() {
    List<String> chartNames = new ArrayList<String>();
    for (ChartName chartName : repo.findAll()) {
      chartNames.add(chartName.getName());
    }
    return chartNames;
  }
}
