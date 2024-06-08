package com.sprinklr.msTeams.mutexBot;

import com.codepoetics.protonpack.collectors.CompletableFutures;
import com.microsoft.bot.builder.BotFrameworkAdapter;
import com.microsoft.bot.builder.MessageFactory;
import com.microsoft.bot.builder.TurnContext;
import com.microsoft.bot.builder.teams.TeamsActivityHandler;
import com.microsoft.bot.builder.teams.TeamsInfo;
import com.microsoft.bot.connector.Async;
import com.microsoft.bot.connector.authentication.MicrosoftAppCredentials;
import com.microsoft.bot.connector.rest.ErrorResponseException;
import com.microsoft.bot.integration.Configuration;
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
import com.microsoft.bot.schema.teams.TeamInfo;
import com.microsoft.bot.schema.teams.TeamsChannelAccount;
import com.sprinklr.msTeams.mutexBot.model.Resource;
import com.sprinklr.msTeams.mutexBot.model.User;
import com.sprinklr.msTeams.mutexBot.service.ResourceService;
import com.sprinklr.msTeams.mutexBot.service.UserService;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.commons.io.IOUtils;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
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

/**
 * This class implements the functionality of the Bot.
 *
 * <p>
 * This is where application specific logic for interacting with the users would
 * be added. For this sample, the {@link #onMessageActivity(TurnContext)} echos
 * the text back to the user. The {@link #onMembersAdded(List, TurnContext)}
 * will send a greeting to new conversation participants.
 * </p>
 */
public class TeamsConversationBot extends TeamsActivityHandler {
  private String appId;
  private String appPassword;
  @Autowired
  private ResourceService resourceService;
  @Autowired
  private UserService userService;
  private static String defaultDuration = "1h";

  private static int timeString2Int(String time) {
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

  public TeamsConversationBot(Configuration configuration) {
    appId = configuration.getProperty("MicrosoftAppId");
    appPassword = configuration.getProperty("MicrosoftAppPassword");
  }

  private static final String ADAPTIVE_CARD_TEMPLATE = "UserMentionCardTemplate.json";

  @Override
  protected CompletableFuture<Void> onMessageActivity(TurnContext turnContext) {
    turnContext.getActivity().removeRecipientMention();

    TeamsChannelAccount user = TeamsInfo.getMember(turnContext, turnContext.getActivity().getFrom().getId()).join();
    String message = turnContext.getActivity().getText().trim().replaceAll(" +", " ");
    String[] message_array = message.split(" ");
    if (message_array.length == 0) {
      return sendMessage(turnContext, "Something went wrong.");
    } else if ((message_array.length != 2) && (message_array.length != 4)) {
      return sendMessage(turnContext, "Invalid Message with " + message_array.length + " words :\n\t" + message);
    }
    String action = message_array[0].toLowerCase();
    String resource = message_array[1];
    int duration;
    if (message.length() == 4) {
      duration = timeString2Int(message_array[3].toLowerCase());
    } else {
      duration = timeString2Int(defaultDuration);
    }

    Activity response = null;
    if (action.equals("reserve")) {
      response = reserveResource(user, resource, duration);
    } else if (action.equals("release")) {
      response = releaseResource(user, resource);
    } else if (action.equals("monitor")) {
      response = monitorResource(user, resource, duration);
    } else if (action.equals("stopmonitoring")) {
      response = stopMonitoringResource(user, resource);
    } else {
      response = MessageFactory.text(
          String.format("Unsure about the action on Resource: \"%s\".\nRecieved action: \"%s\".", resource, action));
    }
    return sendMessage(turnContext, response);

    // if (text.contains("mention me")) {
    // return mentionAdaptiveCardActivityAsync(turnContext);
    // } else if (text.contains("mention")) {
    // return mentionActivity(turnContext);
    // } else if (text.contains("who")) {
    // return getSingleMember(turnContext);
    // } else if (text.contains("message")) {
    // return messageAllMembers(turnContext);
    // } else if (text.contains("update")) {
    // return cardActivity(turnContext, true);
    // } else if (text.contains("delete")) {
    // return deleteCardActivity(turnContext);
    // } else {
    // return cardActivity(turnContext, false);
    // }
  }

  private Activity stopMonitoringResource(TeamsChannelAccount user, String resource) {
    Mention mention = mentionUser(user);
    String userName = (mention != null) ? mention.getText() : user.getName();
    String message = String.format("%s stopped monitoring \"%s\".", userName, resource);
    Activity response = MessageFactory.text(message);
    if (mention != null) {
      response.setMentions(Collections.singletonList(mention));
    }
    return response;
  }

  private Activity monitorResource(TeamsChannelAccount user, String resource, int duration) {
    Mention mention = mentionUser(user);
    String userName = (mention != null) ? mention.getText() : user.getName();
    String message = String.format("%s is monitoring \"%s\" for %d minutes.", userName, resource, duration);
    Activity response = MessageFactory.text(message);
    if (mention != null) {
      response.setMentions(Collections.singletonList(mention));
    }
    return response;
  }

  private Activity releaseResource(TeamsChannelAccount user, String resource_name) {
    Resource resource_obj;
    try {
      resource_obj = resourceService.find(resource_name);
    } catch (Exception e) {
      e.printStackTrace();
      return MessageFactory.text("Exception while fetching resource.");
    }
    if (! resource_obj.isReserved()) {
      return MessageFactory.text(String.format("Resource \"%s\" is not reserved by anyone.", resource_name));
    }
    String user_id = resource_obj.reservedBy;
    if (!user_id.equals(user.getAadObjectId())) {
      return MessageFactory.text(String.format("Resource \"%s\" is not reserved by you, you can't release it.", resource_name));
    }
    resource_obj.release();
    resourceService.save(resource_obj);

    Mention mention = mentionUser(user);
    String userName = (mention != null) ? mention.getText() : user.getName();
    String message = String.format("%s released \"%s\".", userName, resource_name);
    Activity response = MessageFactory.text(message);
    if (mention != null) {
      response.setMentions(Collections.singletonList(mention));
    }
    return response;
  }

  private Activity reserveResource(TeamsChannelAccount user, String resource_name, int duration) {
    Resource resource_obj;
    try {
      resource_obj = resourceService.find(resource_name);
    } catch (Exception e) {
      e.printStackTrace();
      return MessageFactory.text("Exception while fetching resource.");
    }
    if (resource_obj.isReserved()) {
      String user_id = resource_obj.reservedBy;
      User user_obj;
      try {
        user_obj = userService.find(user_id);
      } catch (Exception e) {
        e.printStackTrace();
        return MessageFactory.text("Exception while fetching user.");
      }
      return MessageFactory.text(String.format("Resource \"%s\" is already reserved by %s till %s.", resource_name, user_obj.getName(), resource_obj.reservedTill));
    }
    LocalDateTime reserveTill = LocalDateTime.now().plusMinutes(duration);
    resource_obj.reserve(user.getAadObjectId(), reserveTill);
    resourceService.save(resource_obj);

    if (!userService.exisits(user.getAadObjectId())) {
      userService.save(new User(user.getAadObjectId(), user.getName()));
    }

    Mention mention = mentionUser(user);
    String userName = (mention != null) ? mention.getText() : user.getName();
    String message = String.format("%s reserved \"%s\" till %d.", userName, resource_name, reserveTill);
    Activity response = MessageFactory.text(message);
    if (mention != null) {
      response.setMentions(Collections.singletonList(mention));
    }
    return response;
  }

  private CompletableFuture<Void> sendMessage(TurnContext turnContext, String message) {
    if (message == null) { message = new String(); }
    Activity replyActivity = MessageFactory.text(message);
    return sendMessage(turnContext, replyActivity);
  }

  private Mention mentionUser(ChannelAccount user) {
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

  private CompletableFuture<Void> sendMessage(TurnContext turnContext, Activity message) {
    return turnContext.sendActivity(message).thenApply(resourceResponse -> null);
  }

  @Override
  protected CompletableFuture<Void> onTeamsMembersAdded(
      List<TeamsChannelAccount> membersAdded,
      TeamInfo teamInfo,
      TurnContext turnContext) {
    return membersAdded.stream()
        .filter(
            member -> !StringUtils
                .equals(member.getId(), turnContext.getActivity().getRecipient().getId()))
        .map(
            channel -> turnContext.sendActivity(MessageFactory.text("Welcome to the team " + channel.getGivenName() + " " + channel.getSurname() + ".")))
        .collect(CompletableFutures.toFutureList())
        .thenApply(resourceResponses -> null);
  }

  private CompletableFuture<Void> cardActivity(TurnContext turnContext, Boolean update) {
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

    HeroCard card = new HeroCard();
    List<CardAction> buttons = new ArrayList<>();
    buttons.add(allMembersAction);
    buttons.add(mentionAction);
    buttons.add(mentionMeAction);
    buttons.add(deleteAction);
    card.setButtons(buttons);

    if (update) {
      return sendUpdatedCard(turnContext, card);
    } else {
      return sendWelcomeCard(turnContext, card);
    }
  }

  private CompletableFuture<Void> getSingleMember(TurnContext turnContext) {
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

  private CompletableFuture<Void> deleteCardActivity(TurnContext turnContext) {
    return turnContext.deleteActivity(turnContext.getActivity().getReplyToId());
  }

  private CompletableFuture<Void> messageAllMembers(TurnContext turnContext) {
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

  private static CompletableFuture<Void> sendWelcomeCard(TurnContext turnContext, HeroCard card) {
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

  private static CompletableFuture<Void> sendUpdatedCard(TurnContext turnContext, HeroCard card) {
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

  private CompletableFuture<Void> mentionAdaptiveCardActivityAsync(TurnContext turnContext) {
    return TeamsInfo.getMember(turnContext, turnContext.getActivity().getFrom().getId())
        .thenApply(member -> {
          try (
              InputStream inputStream = Thread.currentThread().getContextClassLoader()
                  .getResourceAsStream(ADAPTIVE_CARD_TEMPLATE);) {
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

  private CompletableFuture<Void> mentionActivity(TurnContext turnContext) {
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
}
