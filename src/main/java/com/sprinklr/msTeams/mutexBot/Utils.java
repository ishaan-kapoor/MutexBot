package com.sprinklr.msTeams.mutexBot;

import com.microsoft.bot.builder.BotFrameworkAdapter;
import com.microsoft.bot.builder.MessageFactory;
import com.microsoft.bot.builder.TurnContext;
import com.microsoft.bot.builder.teams.TeamsInfo;
import com.microsoft.bot.connector.Async;
import com.microsoft.bot.connector.authentication.MicrosoftAppCredentials;
import com.microsoft.bot.connector.rest.ErrorResponseException;
import com.microsoft.bot.schema.ActionTypes;
import com.microsoft.bot.schema.Activity;
import com.microsoft.bot.schema.Attachment;
import com.microsoft.bot.schema.CardAction;
import com.microsoft.bot.schema.ChannelAccount;
import com.microsoft.bot.schema.ConversationParameters;
import com.microsoft.bot.schema.ConversationReference;
import com.microsoft.bot.schema.HeroCard;
import com.microsoft.bot.schema.Mention;
import com.microsoft.bot.schema.Serialization;
import com.microsoft.bot.schema.teams.TeamsChannelAccount;
import com.sprinklr.msTeams.mutexBot.model.User;

import org.apache.commons.io.IOUtils;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Utils {

  public static final String[] actions = { "Reserve", "Release", "Status", "Monitor", "StopMonitoring" };
  public static final String[] adminActions = { "create", "delete", "makeAdmin" };
  private static DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss (dd/MM/yyyy)");
  public static final String ADAPTIVE_CARD_TEMPLATE = "UserMentionCardTemplate.json";
  public static final String DATE_TIME_ADAPTIVE_CARD_TEMPLATE = "/datetime.json";
  public static final String DURATION_ADAPTIVE_CARD_TEMPLATE = "/duration.json";
  public static final String DROPDOWN_ADAPTIVE_CARD_TEMPLATE = "/dropdown.json";
  public static final String UNSURE_ACTION_MESSAGE = "Unsure about the action on Resource: \"%s\".\nRecieved action: \"%s\".";

  // --------------------------------------------------------------------------

  public static String time2link(LocalDateTime time) {
    return String.format(
        "https://www.timeanddate.com/worldclock/fixedtime.html?day=%d&month=%d&year=%d&hour=%d&min=%d&sec=%d",
        time.getDayOfMonth(), time.getMonthValue(), time.getYear(), time.getHour(), time.getMinute(), time.getSecond());
  }

  public static String time2hyperlink(LocalDateTime time) {
    return String.format("[%s UTC](%s)", time.format(timeFormat), time2link(time));
  }

  public static String user2hyperlink(User user, String resource) {
    return String.format("[%s](mailto:%s?subject=Regarding%%20reservation%%20of%%20Jenkins%%20resource%%20\"%s\")",
        user.getName(), user.getEmail(), resource);
  }

  public static String user2hyperlink(TeamsChannelAccount user, String resource) {
    return String.format("[%s](mailto:%s?subject=Regarding%%20reservation%%20of%%20Jenkins%%20resource%%20\"%s\")",
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
    if (message == null) {
      message = new String();
    }
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
    if (mention != null) {
      response.setMentions(Collections.singletonList(mention));
    }
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

  public static CompletableFuture<Void> sendPersonalMessage(String userId, String message, TurnContext turnContext,
      String appId, String appPassword) {
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

  // --------------------------------------------------------------------------

  public CompletableFuture<Void> mentionAdaptiveCardActivityAsync(TurnContext turnContext) {
    return TeamsInfo.getMember(turnContext, turnContext.getActivity().getFrom().getId())
        .thenApply(member -> {
          try (
              InputStream inputStream = Thread.currentThread().getContextClassLoader()
                  .getResourceAsStream(Utils.ADAPTIVE_CARD_TEMPLATE);) {
            String templateJSON = IOUtils
                .toString(inputStream, StandardCharsets.UTF_8.toString());
            String cardJSON = templateJSON.replaceAll("\\$\\{userName\\}", member.getName())
                .replaceAll("\\$\\{userAAD\\}", member.getObjectId())
                .replaceAll("\\$\\{userUPN\\}", member.getUserPrincipalName());

            Attachment adaptiveCardAttachment = new Attachment();
            adaptiveCardAttachment.setContentType("application/vnd.microsoft.card.adaptive");
            adaptiveCardAttachment.setContent(Serialization.jsonToTree(cardJSON));

            return turnContext.sendActivity(MessageFactory.attachment(adaptiveCardAttachment));
          } catch (IOException e) {
            return Async.completeExceptionally(e);
          }
        })
        .exceptionally(ex -> {
          // report member not found cases
          if (ex.getCause() instanceof ErrorResponseException
              && ((ErrorResponseException) ex.getCause()).body().getError().getCode()
                  .equals("MemberNotFoundInConversation")) {
            return turnContext.sendActivity("Member not found.");
          } else {
            // rethrow otherwise
            throw new CompletionException(ex.getCause());
          }
        })
        .thenApply(resourceResponse -> null);
  }

  public CompletableFuture<Void> mentionActivity(TurnContext turnContext) {
    Mention mention = new Mention();
    mention.setMentioned(turnContext.getActivity().getFrom());
    try {
      mention.setText("<at>" + URLEncoder.encode(turnContext.getActivity().getFrom().getName(), "UTF-8") + "</at>");
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }

    Activity replyActivity = MessageFactory.text("Hello " + mention.getText() + ".");
    replyActivity.setMentions(Collections.singletonList(mention));

    return turnContext.sendActivity(replyActivity).thenApply(resourceResponse -> null);
  }

  public CompletableFuture<Void> cardActivity(TurnContext turnContext, Boolean update) {
    CardAction allMembersAction = new CardAction();
    allMembersAction.setType(ActionTypes.MESSAGE_BACK);
    allMembersAction.setTitle("Message all members");
    allMembersAction.setText("MessageAllMembers");

    CardAction mentionAction = new CardAction();
    mentionAction.setType(ActionTypes.MESSAGE_BACK);
    mentionAction.setTitle("Who am I?");
    mentionAction.setText("whoami");

    CardAction mentionMeAction = new CardAction();
    mentionMeAction.setType(ActionTypes.MESSAGE_BACK);
    mentionMeAction.setTitle("Find me in Adaptive Card");
    mentionMeAction.setText("mention me");

    CardAction deleteAction = new CardAction();
    deleteAction.setType(ActionTypes.MESSAGE_BACK);
    deleteAction.setTitle("Delete card");
    deleteAction.setText("Delete");

    CardAction demo = new CardAction();
    demo.setType(ActionTypes.INVOKE);
    demo.setTitle("invoke");
    demo.setText("INVOKKE");

    HeroCard card = new HeroCard();
    List<CardAction> buttons = new ArrayList<>();
    buttons.add(allMembersAction);
    buttons.add(mentionAction);
    buttons.add(mentionMeAction);
    buttons.add(deleteAction);
    buttons.add(demo);
    card.setButtons(buttons);

    if (update) {
      return sendUpdatedCard(turnContext, card);
    } else {
      return sendWelcomeCard(turnContext, card);
    }
  }

  public CompletableFuture<Void> getSingleMember(TurnContext turnContext) {
    return TeamsInfo.getMember(turnContext, turnContext.getActivity().getFrom().getId())
        .thenApply(member -> {
          Activity message = MessageFactory.text(String.format("You are: %s.", member.getName()));
          return turnContext.sendActivity(message);
        })
        .exceptionally(ex -> {
          // report member not found cases
          if (ex.getCause() instanceof ErrorResponseException
              && ((ErrorResponseException) ex.getCause()).body().getError().getCode()
                  .equals("MemberNotFoundInConversation")) {
            return turnContext.sendActivity("Member not found.");
          } else {
            // rethrow otherwise
            throw new CompletionException(ex.getCause());
          }
        })
        .thenApply(resourceResponse -> null);
  }

  public CompletableFuture<Void> deleteCardActivity(TurnContext turnContext) {
    return turnContext.deleteActivity(turnContext.getActivity().getReplyToId());
  }

  public CompletableFuture<Void> messageAllMembers(TurnContext turnContext, String appId, String appPassword) {
    String teamsChannelId = turnContext.getActivity().teamsGetChannelId();
    String serviceUrl = turnContext.getActivity().getServiceUrl();
    MicrosoftAppCredentials credentials = new MicrosoftAppCredentials(appId, appPassword);

    return TeamsInfo.getMembers(turnContext).thenCompose(members -> {
      List<CompletableFuture<Void>> conversations = new ArrayList<>();

      // Send a message to each member. These will all go out
      // at the same time.
      for (TeamsChannelAccount member : members) {
        Activity proactiveMessage = MessageFactory.text(
            "Hello " + member.getGivenName() + " " + member.getSurname()
                + ". I'm a Teams conversation bot.");

        ConversationParameters conversationParameters = new ConversationParameters();
        conversationParameters.setIsGroup(false);
        conversationParameters.setBot(turnContext.getActivity().getRecipient());
        conversationParameters.setMembers(Collections.singletonList(member));
        conversationParameters.setTenantId(turnContext.getActivity().getConversation().getTenantId());

        conversations.add(
            ((BotFrameworkAdapter) turnContext.getAdapter()).createConversation(
                teamsChannelId, serviceUrl, credentials, conversationParameters,
                (context) -> {
                  ConversationReference reference = context.getActivity().getConversationReference();
                  return context.getAdapter()
                      .continueConversation(
                          appId, reference, (inner_context) -> inner_context
                              .sendActivity(proactiveMessage)
                              .thenApply(resourceResponse -> null));
                }));
      }

      return CompletableFuture.allOf(conversations.toArray(new CompletableFuture[0]));
    })
        // After all member messages are sent, send confirmation to the user.
        .thenApply(
            conversations -> turnContext
                .sendActivity(MessageFactory.text("All messages have been sent.")))
        .thenApply(allSent -> null);
  }

  public static CompletableFuture<Void> sendWelcomeCard(TurnContext turnContext, HeroCard card) {
    Object initialValue = new Object() {
      int count = 0;
    };
    card.setTitle("Welcome!");
    CardAction updateAction = new CardAction();
    updateAction.setType(ActionTypes.MESSAGE_BACK);
    updateAction.setTitle("Update Card");
    updateAction.setText("UpdateCardAction");
    updateAction.setValue(initialValue);
    card.getButtons().add(updateAction);

    Activity activity = MessageFactory.attachment(card.toAttachment());

    return turnContext.sendActivity(activity).thenApply(resourceResponse -> null);
  }

  public static CompletableFuture<Void> sendUpdatedCard(TurnContext turnContext, HeroCard card) {
    card.setTitle("I've been updated");
    Map data = (Map) turnContext.getActivity().getValue();
    data.put("count", (int) data.get("count") + 1);
    card.setText("Update count - " + data.get("count"));
    CardAction updateAction = new CardAction();
    updateAction.setType(ActionTypes.MESSAGE_BACK);
    updateAction.setTitle("Update Card");
    updateAction.setText("UpdateCardAction");
    updateAction.setValue(data);
    card.getButtons().add(updateAction);

    Activity activity = MessageFactory.attachment(card.toAttachment());
    activity.setId(turnContext.getActivity().getReplyToId());

    return turnContext.updateActivity(activity).thenApply(resourceResponse -> null);
  }

}
