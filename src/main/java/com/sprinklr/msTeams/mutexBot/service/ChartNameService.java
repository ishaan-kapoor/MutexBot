package com.sprinklr.msTeams.mutexBot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sprinklr.msTeams.mutexBot.model.ChartName;
import com.sprinklr.msTeams.mutexBot.repositories.ChartNameRepository;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChartNameService {

  private final ChartNameRepository repo;

  @Autowired
  public ChartNameService(ChartNameRepository chartNamesRepository) {
    repo = chartNamesRepository;
  }

  public void save(String chartName) {
    repo.save(new ChartName(chartName));
  }

  public void save(ChartName chartName) {
    repo.save(chartName);
  }

  public void delete(String chartName) {
    repo.deleteById(chartName);
  }

  public void delete(ChartName chartName) {
    repo.deleteById(chartName.getName());
  }

  public boolean exists(String name) {
    return repo.existsById(name);
  }

  public List<String> getAll() {
    List<String> chartNames = new ArrayList<String>();
    for (ChartName chartName : repo.findAll()) {
      chartNames.add(chartName.getName());
    }
    return chartNames;
  }
}
