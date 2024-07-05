package com.sprinklr.msTeams.mutexBot.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Represents a chart name entity in the "ChartNames" collection of MongoDB.
 */
@Document(collection = "ChartNames")
public class ChartName {
  @Id
  private String name;

  public ChartName(String name) { this.name = name; }

  public String toString() { return name; }

  public String getName() { return name; }
}
