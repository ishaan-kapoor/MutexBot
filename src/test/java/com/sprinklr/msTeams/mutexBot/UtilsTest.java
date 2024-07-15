package com.sprinklr.msTeams.mutexBot;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.microsoft.bot.builder.MessageFactory;
import com.microsoft.bot.builder.TurnContext;
import com.microsoft.bot.schema.Activity;
import com.microsoft.bot.schema.ChannelAccount;
import com.microsoft.bot.schema.Mention;
import com.microsoft.bot.schema.teams.TeamsChannelAccount;
import com.sprinklr.msTeams.mutexBot.model.User;

class UtilsTest {

  @Mock
  private TurnContext turnContext;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testTime2link() {
    LocalDateTime time = LocalDateTime.of(2024, 7, 15, 10, 30, 45);
    String expected = "https://www.timeanddate.com/worldclock/fixedtime.html?day=15&month=7&year=2024&hour=10&min=30&sec=45";
    assertEquals(expected, Utils.time2link(time));
  }

  @Test
  void testTime2hyperlink() {
    LocalDateTime time = LocalDateTime.of(2024, 7, 15, 10, 30, 45);
    String expected = "[10:30:45 (15/07/2024) UTC](https://www.timeanddate.com/worldclock/fixedtime.html?day=15&month=7&year=2024&hour=10&min=30&sec=45)";
    assertEquals(expected, Utils.time2hyperlink(time));
  }

  @Test
  void testUser2hyperlink_User() {
    User user = new User("user1");
    user.setName("User One");
    user.setEmail("user1@example.com");
    String expected = "[User One](mailto:user1@example.com)";
    assertEquals(expected, Utils.user2hyperlink(user));
  }

  @Test
  void testUser2hyperlink_TeamsChannelAccount() {
    TeamsChannelAccount user = new TeamsChannelAccount();
    user.setName("User One");
    user.setEmail("user1@example.com");
    String expected = "[User One](mailto:user1@example.com)";
    assertEquals(expected, Utils.user2hyperlink(user));
  }

  @Test
  void testUser2hyperlink_UserWithResource() {
    User user = new User("user1");
    user.setName("User One");
    user.setEmail("user1@example.com");
    String resource = "Jenkins Resource";
    String expected = "[User One](mailto:user1@example.com?subject=Regarding%20reservation%20of%20Jenkins%20resource%20\"Jenkins Resource\")";
    assertEquals(expected, Utils.user2hyperlink(user, resource));
  }

  @Test
  void testUser2hyperlink_TeamsChannelAccountWithResource() {
    TeamsChannelAccount user = new TeamsChannelAccount();
    user.setName("User One");
    user.setEmail("user1@example.com");
    String resource = "Jenkins Resource";
    String expected = "[User One](mailto:user1@example.com?subject=Regarding%20reservation%20of%20Jenkins%20resource%20\"Jenkins Resource\")";
    assertEquals(expected, Utils.user2hyperlink(user, resource));
  }

  @Test
  void testTimeString2Int() {
    assertEquals(150, Utils.timeString2Int("2h30m"));
    assertEquals(60, Utils.timeString2Int("1h"));
    assertEquals(45, Utils.timeString2Int("45m"));
    assertEquals(0, Utils.timeString2Int("0m"));
  }

  @Test
  void testSendMessage() {
    Activity message = MessageFactory.text("Test message");
    when(turnContext.sendActivity(message)).thenReturn(CompletableFuture.completedFuture(null));

    CompletableFuture<Void> result = Utils.sendMessage(turnContext, message);

    assertNotNull(result);
    verify(turnContext, times(1)).sendActivity(message);
  }

  @Test
  void testMentionUser() throws UnsupportedEncodingException {
    ChannelAccount user = new ChannelAccount();
    user.setName("User One");

    Mention mention = Utils.mentionUser(user);

    assertNotNull(mention);
    assertEquals(user, mention.getMentioned());
    assertEquals("<at>" + URLEncoder.encode(user.getName(), "UTF-8") + "</at>", mention.getText());
  }
}
