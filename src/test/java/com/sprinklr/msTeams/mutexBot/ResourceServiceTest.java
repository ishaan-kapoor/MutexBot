package com.sprinklr.msTeams.mutexBot;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;

import com.sprinklr.msTeams.mutexBot.model.Resource;
import com.sprinklr.msTeams.mutexBot.repositories.ResourceRepository;
import com.sprinklr.msTeams.mutexBot.service.ResourceService;

@SpringBootTest
public class ResourceServiceTest {

  @Mock
  private ResourceRepository resourceRepository;

  @InjectMocks
  private ResourceService resourceService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    resourceService.refreshCache();
    reset(resourceRepository);
  }

  @Test
  void testGetAll() {
    Resource resource1 = new Resource("Resource1");
    Resource resource2 = new Resource("Resource2");
    when(resourceRepository.findAll()).thenReturn(Arrays.asList(resource1, resource2));

    List<Resource> resources = resourceService.getAll();

    assertEquals(2, resources.size());
    assertTrue(resources.contains(resource1));
    assertTrue(resources.contains(resource2));
    verify(resourceRepository, times(1)).findAll();
  }

  @Test
  void testGetAllNames() {
    Resource resource1 = new Resource("Resource1");
    Resource resource2 = new Resource("Resource2");
    when(resourceRepository.findAll()).thenReturn(Arrays.asList(resource1, resource2));

    List<String> resourceNames = resourceService.getAllNames();

    assertEquals(2, resourceNames.size());
    assertTrue(resourceNames.contains("Resource1"));
    assertTrue(resourceNames.contains("Resource2"));
    verify(resourceRepository, times(1)).findAll();
  }

  @Test
  void testGetReserved() {
    Resource resource1 = new Resource("Resource1");
    resource1.reserve("user1", LocalDateTime.now().plusHours(1));
    when(resourceRepository.findReservedResources(any(LocalDateTime.class)))
        .thenReturn(Collections.singletonList(resource1));

    List<Resource> reservedResources = resourceService.getReserved();

    assertEquals(1, reservedResources.size());
    assertTrue(reservedResources.contains(resource1));
    verify(resourceRepository, times(1)).findReservedResources(any(LocalDateTime.class));
  }

  @Test
  void testGetAvailable() {
    Resource resource1 = new Resource("Resource1");
    Resource resource2 = new Resource("Resource2");
    when(resourceRepository.findAvailableResources(any(LocalDateTime.class)))
        .thenReturn(Arrays.asList(resource1, resource2));

    List<Resource> availableResources = resourceService.getAvailable();

    assertEquals(2, availableResources.size());
    assertTrue(availableResources.contains(resource1));
    assertTrue(availableResources.contains(resource2));
    verify(resourceRepository, times(1)).findAvailableResources(any(LocalDateTime.class));
  }

  @Test
  void testFind() throws Exception {
    Resource resource = new Resource("Resource1");
    when(resourceRepository.findById("Resource1")).thenReturn(Optional.of(resource));

    Resource foundResource = resourceService.find("Resource1");

    assertNotNull(foundResource);
    assertEquals("Resource1", foundResource.getName());
    verify(resourceRepository, times(1)).findById("Resource1");
  }

  @Test
  void testSaveNewResource() {
    resourceService.save("NewResource");

    verify(resourceRepository, times(1)).save(any(Resource.class));
  }

  @Test
  void testSaveResource() {
    Resource resource = new Resource("Resource1");
    resourceService.save(resource);

    verify(resourceRepository, times(1)).save(resource);
  }

  @Test
  void testExists() {
    Resource resource = new Resource("Resource1");
    when(resourceRepository.findAll()).thenReturn(Collections.singletonList(resource));
    resourceService.refreshCache();

    assertTrue(resourceService.exists("Resource1"));
    assertFalse(resourceService.exists("NonExistentResource"));
  }

  @Test
  void testDelete() {
    resourceService.delete("Resource1");

    verify(resourceRepository, times(1)).deleteById("Resource1");
  }

  @Test
  void testFindByChartName() {
    Resource resource = new Resource("Chart-Resource1");
    when(resourceRepository.findByIdStartingWith("Chart-")).thenReturn(Collections.singletonList(resource));

    List<String> resources = resourceService.findByChartName("Chart");

    assertEquals(1, resources.size());
    assertTrue(resources.contains("Chart-Resource1"));
    verify(resourceRepository, times(1)).findByIdStartingWith("Chart-");
  }
}
