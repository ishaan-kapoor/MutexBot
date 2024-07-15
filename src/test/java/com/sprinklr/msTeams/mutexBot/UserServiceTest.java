package com.sprinklr.msTeams.mutexBot;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;

import com.sprinklr.msTeams.mutexBot.model.User;
import com.sprinklr.msTeams.mutexBot.repositories.UserRepository;
import com.sprinklr.msTeams.mutexBot.service.UserService;

@SpringBootTest
public class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private UserService userService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    reset(userRepository);
  }

  @Test
  void testGetAll() {
    User user1 = new User("User1");
    User user2 = new User("User2");
    when(userRepository.findAll()).thenReturn(Arrays.asList(user1, user2));

    List<User> users = userService.getAll();

    assertEquals(2, users.size());
    assertTrue(users.contains(user1));
    assertTrue(users.contains(user2));
    verify(userRepository, times(1)).findAll();
  }

  @Test
  void testFind() throws Exception {
    User user = new User("User1");
    when(userRepository.findById("User1")).thenReturn(Optional.of(user));

    User foundUser = userService.find("User1");

    assertNotNull(foundUser);
    assertEquals("User1", foundUser.getId());
    verify(userRepository, times(1)).findById("User1");
  }

  @Test
  void testFindByEmail() {
    User user = new User("User1");
    user.setEmail("test@example.com");
    when(userRepository.findByEmail("test@example.com")).thenReturn(user);

    User foundUser = userService.findByEmail("test@example.com");

    assertNotNull(foundUser);
    assertEquals("test@example.com", foundUser.getEmail());
    verify(userRepository, times(1)).findByEmail("test@example.com");
  }

  @Test
  void testExists_String() throws Exception {
    User user = new User("User1");
    user.setEmail("test@example.com");
    user.setName("Test User");
    when(userRepository.existsById("User1")).thenReturn(true);
    when(userRepository.findById("User1")).thenReturn(Optional.of(user));

    boolean exists = userService.exists("User1");

    assertTrue(exists);
    verify(userRepository, times(1)).existsById("User1");
    verify(userRepository, times(1)).findById("User1");
  }

  @Test
  void testExists_UserDoesNotExist() {
    when(userRepository.existsById("NonExistentUser")).thenReturn(false);

    boolean exists = userService.exists("NonExistentUser");

    assertFalse(exists);
    verify(userRepository, times(1)).existsById("NonExistentUser");
  }

  @Test
  void testSave() {
    User user = new User("User1");

    userService.save(user);

    verify(userRepository, times(1)).save(user);
  }
}
