package com.sprinklr.msTeams.mutexBot;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sprinklr.msTeams.mutexBot.service.ChartNameService;
import com.sprinklr.msTeams.mutexBot.service.ResourceService;

import org.springframework.scheduling.annotation.Async;
import java.util.concurrent.CompletableFuture;

/**
 * The HelmCharts class is responsible for synchronizing the local database with
 * the Helm charts repository in GitLab.
 * It retrieves the directory structure from the GitLab repository and updates
 * the local database accordingly.
 */
@Service
public class HelmCharts {
  @Value("${gitlab.token}")
  private String privateToken;

  @Value("${gitlab.projectId}")
  private String projectId;

  private String baseUrl;

  private List<String> chartNames = new ArrayList<String>();
  private List<String> resourceNames = new ArrayList<String>();

  private final ResourceService resourceService;
  private final ChartNameService chartNameService;

  /**
   * Constructs a HelmCharts instance with the specified services.
   *
   * @param resourceService  The service responsible for resource-related
   *                         operations.
   * @param chartNameService The service responsible for chart name operations.
   */
  @Autowired
  public HelmCharts(ResourceService resourceService, ChartNameService chartNameService) {
    this.resourceService = resourceService;
    this.chartNameService = chartNameService;
  }

  /**
   * Initializes the base URL for the GitLab repository and triggers an initial
   * synchronization.
   */
  @PostConstruct
  public void init() {
    baseUrl = "https://prod-gitlab.sprinklr.com/api/v4/projects/" + projectId + "/repository/tree";
    syncDB();
  }

  /**
   * Asynchronously retrieves the list of releases for a given chart.
   *
   * @param chartName The name of the chart.
   * @return A CompletableFuture containing the list of releases.
   */
  @Async
  private CompletableFuture<List<String>> getReleases(String chartName) {
    List<String> releases = new ArrayList<>();
    Optional<List<JsonObject>> releasesOptional = getTree("charts/" + chartName + "/releases");
    if (!releasesOptional.isPresent()) {
      System.err.println("Invalid path: charts/" + chartName + "/releases");
    }
    for (JsonObject release : releasesOptional.get()) {
      if (!release.get("type").getAsString().equals("tree")) { continue; }
      releases.add(chartName + "-" + release.get("name").getAsString());
    }
    return CompletableFuture.completedFuture(releases);
  }

  /**
   * Retrieves the repository structure from GitLab and updates the local lists of
   * chart and resource names.
   */
  public synchronized void getRepo() {
    chartNames.clear();
    resourceNames.clear();

    Optional<List<JsonObject>> chartsOptional = getTree("charts");
    if (!chartsOptional.isPresent()) {
      System.err.println("Invalid path: charts");
      return;
    }

    List<JsonObject> charts = chartsOptional.get();
    List<CompletableFuture<List<String>>> futures = new ArrayList<>();

    for (JsonObject chart : charts) {
      if (!chart.getAsJsonObject().get("type").getAsString().equals("tree")) { continue; }
      String chartName = chart.get("name").getAsString();
      chartNames.add(chartName);
      futures.add(getReleases(chartName));
    }

    // Collect all release names
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    for (CompletableFuture<List<String>> future : futures) {
      try {
        resourceNames.addAll(future.get());
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Retrieves the tree structure of the specified path from the GitLab
   * repository.
   *
   * @param path The path to retrieve the tree structure for.
   * @return An Optional containing the list of JsonObjects representing the tree
   *         structure.
   */
  private Optional<List<JsonObject>> getTree(String path) {
    HttpClient client = HttpClient.newHttpClient();
    List<JsonObject> allItems = new ArrayList<>();
    int page = 1;
    String url = baseUrl + "?path=" + path;
    URI uri;
    HttpResponse<String> response;
    while (true) {
      try {
        uri = new URI(url + "&per_page=100&page=" + page);
      } catch (URISyntaxException e) {
        System.err.println("Invalid uri: " + url);
        return Optional.empty();
      }
      HttpRequest request = HttpRequest.newBuilder().uri(uri).header("PRIVATE-TOKEN", privateToken).build();

      try {
        response = client.send(request, HttpResponse.BodyHandlers.ofString());
      } catch (IOException e) {
        System.err.println("IO Exception in HelmCharts service");
        e.printStackTrace();
        return Optional.empty();
      } catch (InterruptedException e) {
        System.err.println("Request Interrupted in HelmCharts service");
        return Optional.empty();
      }

      // Get all pages
      if (response.statusCode() == 200) {
        JsonArray items = JsonParser.parseString(response.body()).getAsJsonArray();
        if (items.size() == 0) { break; }  // No more items
        items.forEach(item -> allItems.add(item.getAsJsonObject()));
        page++;
      } else {
        System.err.println("Failed to fetch tree at path: " + path + ", Status code: " + response.statusCode());
        return Optional.empty();
      }
    }
    return Optional.of(allItems);
  }

  /**
   * Schedules the synchronization of the local database with the GitLab
   * repository every hour.
   */
  @Scheduled(cron = "0 0 * * * ?")
  public synchronized void syncDB() {
    System.out.println("\nGetting Repo");
    getRepo();
    System.out.println("\nGot Repo");

    Set<String> dbResources = new HashSet<String>(resourceService.getAllNames());
    Set<String> newResources = new HashSet<String>(resourceNames);
    newResources.removeAll(dbResources);
    newResources.forEach(resourceName -> resourceService.save(resourceName));
    Set<String> deletedResources = new HashSet<String>(dbResources);
    deletedResources.removeAll(resourceNames);
    deletedResources.forEach(resourceName -> resourceService.delete(resourceName));

    Set<String> dbCharts = new HashSet<String>(chartNameService.getAll());
    Set<String> newCharts = new HashSet<String>(chartNames);
    newCharts.removeAll(dbCharts);
    newCharts.forEach(chartName -> chartNameService.save(chartName));
    Set<String> deletedCharts = new HashSet<String>(dbCharts);
    deletedCharts.removeAll(chartNames);
    deletedCharts.forEach(chartName -> chartNameService.delete(chartName));

    System.out.println("\nSynced DB");
  }
}
