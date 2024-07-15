package com.sprinklr.msTeams.mutexBot;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Value;

import com.microsoft.bot.builder.TurnContext;
import com.microsoft.bot.builder.teams.TeamsInfo;
import com.microsoft.bot.schema.Activity;
import com.microsoft.bot.schema.ChannelAccount;
import com.microsoft.bot.schema.teams.TeamsChannelAccount;
import com.sprinklr.msTeams.mutexBot.model.Resource;
import com.sprinklr.msTeams.mutexBot.model.User;
import com.sprinklr.msTeams.mutexBot.service.ChartNameService;
import com.sprinklr.msTeams.mutexBot.service.MonitorLogService;
import com.sprinklr.msTeams.mutexBot.service.ReservationLogService;
import com.sprinklr.msTeams.mutexBot.service.ResourceService;
import com.sprinklr.msTeams.mutexBot.service.UserService;

class ActionsTest {

  @Mock
  private ResourceService resourceService;

  @Mock
  private UserService userService;

  @Mock
  private UserInput userInput;

  @Mock
  private ReservationLogService reservationLogService;

  @Mock
  private MonitorLogService monitorLogService;

  @Mock
  private ChartNameService chartNameService;

  @Mock
  private HelmCharts helmConnector;

  @Mock
  private TurnContext turnContext;

  @Mock
  private TeamsChannelAccount teamsUser;

  @Mock
  private Resource resource;

  @Mock
  private User user;

  @Value("${MicrosoftAppId}")
  private String appId;

  @Value("${MicrosoftAppPassword}")
  private String appPassword;

  private Actions actions;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    actions = new Actions(resourceService, userService, reservationLogService, monitorLogService,
        chartNameService, userInput, helmConnector, appId, appPassword);
  }

  @Test
  void testGetStatus_Reserved() throws Exception {
    when(resource.isReserved()).thenReturn(true);
    when(resource.getReservedBy()).thenReturn("another-user-id");
    when(userService.find("another-user-id")).thenReturn(user);
    when(resource.getReservedTill()).thenReturn(LocalDateTime.now().plusMinutes(30));

    Activity response = actions.getStatus(resource);

    assertNotNull(response);
    assertTrue(response.getText().contains("is reserved by"));
  }

  @Test
  void testGetStatus_NotReserved() {
    when(resource.isReserved()).thenReturn(false);
    when(resource.getName()).thenReturn("test-resource");

    Activity response = actions.getStatus(resource);

    assertNotNull(response);
    assertTrue(response.getText().contains("is not reserved by anyone"));
  }

}
