package com.sprinklr.msTeams.mutexBot;

import com.microsoft.bot.builder.BotFrameworkAdapter;
import com.microsoft.bot.builder.MessageFactory;
import com.microsoft.bot.builder.TurnContext;
import com.microsoft.bot.connector.authentication.MicrosoftAppCredentials;
import com.microsoft.bot.schema.Activity;
import com.microsoft.bot.schema.ChannelAccount;
import com.microsoft.bot.schema.ConversationParameters;
import com.microsoft.bot.schema.ConversationReference;
import com.microsoft.bot.schema.Mention;
import com.microsoft.bot.schema.teams.TeamsChannelAccount;

import com.sprinklr.msTeams.mutexBot.model.User;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

public class Utils {

  public static final String[] actions = { "Reserve", "Release", "Status", "Monitor", "StopMonitoring" };
  public static final String[] adminActions = { "createResource", "deleteResource", "makeAdmin", "dismissAdmin", "forceRelease", "createChartName", "deleteChartName", "resourceLog", "userLog", "sync" };
  public static DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss (dd/MM/yyyy)");
  public static final String DURATION_ADAPTIVE_CARD_TEMPLATE = "/duration.json";
  public static final String ADMIN_ACTIONS_ADAPTIVE_CARD_TEMPLATE = "/adminActions.json";
  public static final String RESOURCE_ADAPTIVE_CARD_TEMPLATE = "/resourceDropDown.json";
  public static final String UNSURE_ACTION_MESSAGE = "Unsure about the action on Resource: \"%s\".\nRecieved action: \"%s\".";

  public static final String URL = "http://localhost:3978/";

  // --------------------------------------------------------------------------

  public static String time2link(LocalDateTime time) {
    return String.format(
        "https://www.timeanddate.com/worldclock/fixedtime.html?day=%d&month=%d&year=%d&hour=%d&min=%d&sec=%d",
        time.getDayOfMonth(), time.getMonthValue(), time.getYear(), time.getHour(), time.getMinute(), time.getSecond());
  }

  public static String time2hyperlink(LocalDateTime time) {
    return String.format("[%s UTC](%s)", time.format(timeFormat), time2link(time));
  }

  public static String user2hyperlink(User user) {
    return String.format("[%s](mailto:%s)", user.getName(), user.getEmail());
  }

  public static String user2hyperlink(TeamsChannelAccount user) {
    return String.format("[%s](mailto:%s)", user.getName(), user.getEmail());
  }

  public static String user2hyperlink(User user, String resource) {
    return String.format(
        "[%s](mailto:%s?subject=Regarding%%20reservation%%20of%%20Jenkins%%20resource%%20\"%s\")",
        user.getName(), user.getEmail(), resource);
  }

  public static String user2hyperlink(TeamsChannelAccount user, String resource) {
    return String.format(
        "[%s](mailto:%s?subject=Regarding%%20reservation%%20of%%20Jenkins%%20resource%%20\"%s\")",
        user.getName(), user.getEmail(), resource);
  }

  public static int timeString2Int(String time) {
    int hours = 0, minutes = 0;

    String[] parts = time.split("(?<=\\D)(?=\\d)");
    for (String part : parts) {
      if (part.endsWith("h")) {
        hours = Integer.parseInt(part.substring(0, part.length() - 1));
      } else if (part.endsWith("m")) {
        minutes = Integer.parseInt(part.substring(0, part.length() - 1));
      }
    }

    return hours * 60 + minutes;
  }

  // --------------------------------------------------------------------------

  public static CompletableFuture<Void> sendMessage(TurnContext turnContext, String message) {
    if (message == null) { message = new String(); }
    Activity replyActivity = MessageFactory.text(message);
    return sendMessage(turnContext, replyActivity);
  }

  public static CompletableFuture<Void> sendMessage(TurnContext turnContext, Activity message) {
    return turnContext.sendActivity(message).thenApply(resourceResponse -> null);
  }

  public static Activity makeMentionedResponse(TeamsChannelAccount user, String message) {
    Mention mention = mentionUser(user);
    String userName = (mention != null) ? mention.getText() : user.getName();
    Activity response = MessageFactory.text(userName + message);
    if (mention != null) { response.setMentions(Collections.singletonList(mention)); }
    return response;
  }

  public static Mention mentionUser(ChannelAccount user) {
    Mention mention = new Mention();
    mention.setMentioned(user);
    try {
      mention.setText("<at>" + URLEncoder.encode(user.getName(), "UTF-8") + "</at>");
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
      return null;
    }
    return mention;
  }

  public static CompletableFuture<Void> sendPersonalMessage(String userId, String message, TurnContext turnContext, String appId, String appPassword) {
    String teamsChannelId = turnContext.getActivity().teamsGetChannelId();
    String serviceUrl = turnContext.getActivity().getServiceUrl();
    MicrosoftAppCredentials credentials = new MicrosoftAppCredentials(appId, appPassword);
    ChannelAccount user = new ChannelAccount(userId);
    // TeamsChannelAccount user = TeamsInfo.getMember(turnContext, userId).join();

    Activity response = MessageFactory.text(message);
    ConversationParameters conversationParameters = new ConversationParameters();
    conversationParameters.setIsGroup(false);
    conversationParameters.setBot(turnContext.getActivity().getRecipient());
    conversationParameters.setMembers(Collections.singletonList(user));
    conversationParameters.setTenantId(turnContext.getActivity().getConversation().getTenantId());

    return ((BotFrameworkAdapter) turnContext.getAdapter()).createConversation(
        teamsChannelId, serviceUrl, credentials, conversationParameters,
        (context) -> {
          ConversationReference reference = context.getActivity().getConversationReference();
          return context.getAdapter()
              .continueConversation(
                  appId, reference, (inner_context) -> inner_context
                      .sendActivity(response)
                      .thenApply(resourceResponse -> null));
        });
  }

}
