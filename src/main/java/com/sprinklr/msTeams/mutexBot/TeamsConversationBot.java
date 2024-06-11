package com.sprinklr.msTeams.mutexBot;

import com.codepoetics.protonpack.collectors.CompletableFutures;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.bot.builder.BotFrameworkAdapter;
import com.microsoft.bot.builder.InvokeResponse;
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
import com.microsoft.bot.schema.ActivityTypes;
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
import com.sprinklr.msTeams.mutexBot.utils.UserTimeEntry;

import org.apache.commons.lang3.StringUtils;
import org.bson.codecs.pojo.PropertyAccessor;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
  private static final String[] adminActions = {"create", "delete", "makeAdmin"};
  private static DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss (dd/MM/yyyy)");
  private static String helpMessage = "Commands:<br> &emsp;Reserve \\<Resource\\> [for \\<Duration\\>]<br> &emsp;Release \\<Resource\\><br> &emsp;Status \\<Resource\\><br> &emsp;Monitor \\<Resource\\> [for \\<Duration\\>]<br> &emsp;StopMonitoring \\<Resource\\><br>Where \\<Duration\\> is in the form of \\<hours\\>h\\<minutes\\>m, and default duration is for 1 hour.\ne.g.<br> &emsp;Reserve prod:qa6 for 12m<br> &emsp;StopMonitoring dev:qa6";

  private static String time2link(LocalDateTime time) {
    return String.format("https://www.timeanddate.com/worldclock/fixedtime.html?day=%d&month=%d&year=%d&hour=%d&min=%d&sec=%d", time.getDayOfMonth(), time.getMonthValue(), time.getYear(), time.getHour(), time.getMinute(), time.getSecond());
  }

  private static String time2hyperlink(LocalDateTime time) {
    return String.format("[%s UTC](%s)", time.format(timeFormat), time2link(time));
  }

  private static String user2hyperlink(User user, String resource) {
    return String.format("[%s](mailto:%s?subject=Regarding%%20reservation%%20of%%20Jenkins%%20resource%%20\"%s\")", user.getName(), user.getEmail(), resource);
  }

  private static String user2hyperlink(TeamsChannelAccount user, String resource) {
    return String.format("[%s](mailto:%s?subject=Regarding%%20reservation%%20of%%20Jenkins%%20resource%%20\"%s\")", user.getName(), user.getEmail(), resource);
  }

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
  private static final String DATE_TIME_ADAPTIVE_CARD_TEMPLATE = "/datetime.json";
  private static final String DURATION_ADAPTIVE_CARD_TEMPLATE = "/duration.json";
  private static final String DROPDOWN_ADAPTIVE_CARD_TEMPLATE = "/dropdown.json";
  private static final String UNSURE_ACTION_MESSAGE = "Unsure about the action on Resource: \"%s\".\nRecieved action: \"%s\".";

  private CompletableFuture<Void> resourceSelection(TurnContext turnContext) {
    List<Resource> resources = resourceService.getAll();
    // List<CardAction> buttons = new ArrayList<>();
    // for (Resource resource: resources) {
    //   CardAction cardAction = new CardAction();
    //   cardAction.setType(ActionTypes.MESSAGE_BACK);
    //   cardAction.setTitle(resource.getName());
    //   cardAction.setText("* " + resource.getName());
    //   buttons.add(cardAction);
    // }
    // HeroCard card = new HeroCard();
    // card.setButtons(buttons);
    // card.setTitle("Choose the resource to perform action on.");
    // return sendMessage(turnContext, MessageFactory.attachment(card.toAttachment()));

    JsonNode content;
    try(InputStream inputStream = getClass().getResourceAsStream(DROPDOWN_ADAPTIVE_CARD_TEMPLATE)) {
      if (inputStream == null) {
        return sendMessage(turnContext, "Error while loading adaptive card.<br>(CODE: json file for card not found)");
      }
      String templateJSON = IOUtils.toString(inputStream, StandardCharsets.UTF_8.toString());

      StringBuilder choicesBuilder = new StringBuilder();
      for (Resource resource : resources) {
        if (choicesBuilder.length() > 0) {
          choicesBuilder.append(", ");
        }
        choicesBuilder.append(String.format("{\"title\": \"%s\", \"value\": \"%s\"}", resource.getName(), resource.getName()));
      }
      String cardJSON = templateJSON.replace("{}", choicesBuilder.toString());
      content = Serialization.jsonToTree(cardJSON);
    } catch (IOException e) {
      e.printStackTrace();
      return sendMessage(turnContext, "Error while loading adaptive card.<br>"+e);
    }

    Attachment adaptiveCardAttachment = new Attachment();
    adaptiveCardAttachment.setContentType("application/vnd.microsoft.card.adaptive");
    adaptiveCardAttachment.setContent(content);
    return sendMessage(turnContext, MessageFactory.attachment(adaptiveCardAttachment));
  }

  private CompletableFuture<Void> durationSelection(TurnContext turnContext, String resource, String action) {
    JsonNode content;
    try(InputStream inputStream = getClass().getResourceAsStream(DURATION_ADAPTIVE_CARD_TEMPLATE)) {
      if (inputStream == null) {
        return sendMessage(turnContext, "Error while loading adaptive card.<br>(CODE: json file for card not found)");
      }
      String templateJSON = IOUtils.toString(inputStream, StandardCharsets.UTF_8.toString());
      String cardJSON = templateJSON.replaceAll("\\$\\{resource\\}", resource).replaceAll("\\$\\{action\\}", action);
      content = Serialization.jsonToTree(cardJSON);
    } catch (IOException e) {
      e.printStackTrace();
      return sendMessage(turnContext, "Error while loading adaptive card.<br>"+e);
    }

    Attachment adaptiveCardAttachment = new Attachment();
    adaptiveCardAttachment.setContentType("application/vnd.microsoft.card.adaptive");
    adaptiveCardAttachment.setContent(content);
    return sendMessage(turnContext, MessageFactory.attachment(adaptiveCardAttachment));
  }

  private CompletableFuture<Void> actionSelection(TurnContext turnContext, String resource) {
    String[] actions = {"Reserve", "Release", "Status", "Monitor", "StopMonitoring"};
    List<CardAction> buttons = new ArrayList<>();
    for (String action: actions) {
      CardAction cardAction = new CardAction();
      cardAction.setType(ActionTypes.MESSAGE_BACK);
      cardAction.setTitle(action);
      cardAction.setText(action + " " + resource);
      buttons.add(cardAction);
    }
    HeroCard card = new HeroCard();
    card.setButtons(buttons);
    card.setTitle("Choose the action to perform on \"" + resource + "\".");
    return sendMessage(turnContext, MessageFactory.attachment(card.toAttachment()));
  }

  protected CompletableFuture<Void> actOnResource(TurnContext turnContext, String resource_name) {
    Resource resource;
    try {
      resource = resourceService.find(resource_name);
    } catch (Exception e) {
      e.printStackTrace();
      return sendMessage(turnContext, "Exception while fetching resource.");
    }
    if (resource == null) {
      return sendMessage(turnContext, "Resource \"" + resource_name + "\" not found.");
    }
    return actOnResource(turnContext, resource_name, "*");
  }

  protected CompletableFuture<Void> actOnResource(TurnContext turnContext, String resource_name, String action) {
    for (String act: adminActions) {
      if (act.equals(action)) {
        return adminAction(turnContext, resource_name, action);
      }
    }

    Resource resource;
    try {
      resource = resourceService.find(resource_name);
    } catch (Exception e) {
      e.printStackTrace();
      return sendMessage(turnContext, "Exception while fetching resource.");
    }
    if (resource == null) {
      return sendMessage(turnContext, "Resource \"" + resource_name + "\" not found.");
    }

    if (action.equals("*")) {
      return actionSelection(turnContext, resource_name);
    } else if ((action.equals("reserve")) || action.equals("monitor")) {
      return durationSelection(turnContext, resource_name, action);
    }

    TeamsChannelAccount user = TeamsInfo.getMember(turnContext, turnContext.getActivity().getFrom().getId()).join();
    if (!userService.exists(user)) { userService.save(new User(user)); }

    Activity response;
    if (action.equals("release")) {
      response = releaseResource(user, turnContext, resource);
    } else if (action.equals("stopmonitoring")) {
      response = stopMonitoringResource(user, resource);
    } else if (action.equals("status")) {
      response = getStatus(resource);
    } else {
      response = MessageFactory.text(String.format(UNSURE_ACTION_MESSAGE, resource_name, action));
    }
    return sendMessage(turnContext, response);
  }

  protected CompletableFuture<Void> adminAction(TurnContext turnContext, String resource_name, String action) {
    String user_id = turnContext.getActivity().getFrom().getId();
    TeamsChannelAccount teamsUser = TeamsInfo.getMember(turnContext, user_id).join();
    User user;
    try {
      user = userService.find(user_id);
    } catch (Exception e) {
      e.printStackTrace();
      return sendMessage(turnContext, "Exception while fetching user.");
    }
    if ((user == null)) {
      userService.save(new User(teamsUser));
      return sendMessage(turnContext, "Only admins can create/delete resources");
    }
    if (!userService.exists(user_id)) { userService.save(new User(teamsUser)); }
    if (!user.isAdmin()) {
      return sendMessage(turnContext, "Only admins can create/delete resources");
    }

    boolean exists = resourceService.exists(resource_name);
    if (action.equals("create")) {
      if (exists) {
        return sendMessage(turnContext, "Resource \"" + resource_name + "\" already exists.");
      }
      resourceService.save(new Resource(resource_name));
      return sendMessage(turnContext, "Resource \"" + resource_name + "\" created successfully.");
    } else if (action.equals("delete")) {
      if (!exists) {
        return sendMessage(turnContext, "Resource \"" + resource_name + "\" not found.");
      }
      resourceService.delete(resource_name);
      return sendMessage(turnContext, "Resource \"" + resource_name + "\" deleted successfully.");
    } else {
      return sendMessage(turnContext, "Invalid admin action: "+action);
    }
  }

  protected CompletableFuture<Void> actOnResource(TurnContext turnContext, String resource_name, String action, Integer duration) {
    Resource resource;
    try {
      resource = resourceService.find(resource_name);
    } catch (Exception e) {
      e.printStackTrace();
      return sendMessage(turnContext, "Exception while fetching resource.");
    }
    if (resource == null) {
      return sendMessage(turnContext, "Resource \"" + resource_name + "\" not found.");
    }

    TeamsChannelAccount user = TeamsInfo.getMember(turnContext, turnContext.getActivity().getFrom().getId()).join();
    if (!userService.exists(user)) { userService.save(new User(user)); }

    if (duration <= 0) {
      return sendMessage(turnContext, MessageFactory.text("Duration can't be -ve or zero"));
    }

    Activity response;
    if (action.equals("reserve")) {
      response = reserveResource(user, turnContext, resource, duration);
    } else if (action.equals("monitor")) {
      response = monitorResource(user, resource, duration);
    } else {
      response = MessageFactory.text(String.format(UNSURE_ACTION_MESSAGE, resource_name, action));
    }
    return sendMessage(turnContext, response);
  }

  @Override
  protected CompletableFuture<Void> onMessageActivity(TurnContext turnContext) {
    turnContext.getActivity().removeRecipientMention();

    System.out.println("\n"+turnContext.getActivity().getLocalTimezone()+"\n");
    if (turnContext.getActivity().getText() == null) {
      Map<String, Object> data = (Map<String, Object>) turnContext.getActivity().getValue();
      String card = (String) data.get("card");
      // Check if the activity is from the specific Adaptive Card
      if (card == null) {
        return sendMessage(turnContext, "No message recieved.");
      }
      if (card.equals("durationCard")) {
        Integer hours = Integer.parseInt((String) data.get("hours"));
        Integer minutes = Integer.parseInt((String) data.get("minutes"));
        String resource = (String) data.get("resource");
        String action = (String) data.get("action");

        if (hours != null && minutes != null && resource != null && action != null) {
          return actOnResource(turnContext, resource, action, hours*60+minutes);
        } else {
          return sendMessage(turnContext, "Please enter valid values.");
        }
      } else if (card.equals("resourceCard")) {
        String resource = (String) data.get("resource");
        if (resource == null) {
          return sendMessage(turnContext, "Please enter a valid resource name.");
        }
        return actOnResource(turnContext, resource);
      }
      return sendMessage(turnContext, "No message recieved.");
    }

    String message = turnContext.getActivity().getText().trim().replaceAll(" +", " ");
    String[] message_array = message.split(" ");
    if (message_array.length == 0) {
      return sendMessage(turnContext, "Something went wrong.");
    } else if ( (message_array.length == 1) && (message.toLowerCase().equals("help")) ) {
      return sendMessage(turnContext, helpMessage);
    } else if ( (message_array.length == 1) && (message.toLowerCase().equals("act")) ) {
      return resourceSelection(turnContext);
    } else if (message_array.length == 2) {
      return actOnResource(turnContext, message_array[1], message_array[0].toLowerCase());
    } else if (message_array.length == 4) {
      return actOnResource(turnContext, message_array[1], message_array[0].toLowerCase(), timeString2Int(message_array[3].toLowerCase()));
    }
    sendMessage(turnContext, "Invalid message recieved.<br>"+message);
    return resourceSelection(turnContext);

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

  private Activity stopMonitoringResource(TeamsChannelAccount user, Resource resource) {
    resource.stopMonitoring(user.getId());
    resourceService.save(resource);

    String message = String.format(" stopped monitoring \"%s\".", resource.getName());
    return makeMentionedResponse(user, message);
  }

  private Activity monitorResource(TeamsChannelAccount user, Resource resource, int duration) {
    LocalDateTime monitorTill = LocalDateTime.now().plusMinutes(duration);
    resource.monitor(user.getId(), monitorTill);
    resourceService.save(resource);

    String message = String.format(" is monitoring \"%s\" till %s.", resource.getName(), time2hyperlink(monitorTill));
    return makeMentionedResponse(user, message);
  }

  private Activity releaseResource(TeamsChannelAccount user, TurnContext turnContext, Resource resource) {
    if (!resource.isReserved()) {
      return MessageFactory.text(String.format("Resource \"%s\" is not reserved by anyone.", resource.getName()));
    }

    String reservingUserId = resource.reservedBy;
    if (! user.getId().equals(reservingUserId)) {
      User reservingUser;
      try {
        reservingUser = userService.find(reservingUserId);
      } catch (Exception e) {
        System.out.println("Error while fetching monitoring user");
        e.printStackTrace();
        reservingUser = new User(reservingUserId);
      }
      return MessageFactory.text(String.format("Resource \"%s\" is reserved by %s", resource.getName(), user2hyperlink(reservingUser, resource.getName())));
    }

    resource.release();
    resource.clean_monitor_list();
    resourceService.save(resource);

    String message = String.format(" released \"%s\".", resource.getName());

    for (UserTimeEntry entry: resource.monitoredBy) {
      sendPersonalMessage(entry.user, user2hyperlink(user, resource.getName()) + message, turnContext).join();
    }

    return makeMentionedResponse(user, message);
  }

  private Activity reserveResource(TeamsChannelAccount user, TurnContext turnContext, Resource resource, int duration) {
    if (resource.isReserved()) {
      User reservingUser;
      try {
        reservingUser = userService.find(resource.reservedBy);
      } catch (Exception e) {
        e.printStackTrace();
        return MessageFactory.text("Exception while fetching user.");
      }
      return MessageFactory.text(String.format("Resource \"%s\" is already reserved by %s till %s.", resource.getName(), user2hyperlink(reservingUser, resource.getName()), time2hyperlink(resource.reservedTill)));
    }

    LocalDateTime reserveTill = LocalDateTime.now().plusMinutes(duration);
    resource.reserve(user.getId(), reserveTill);
    resource.clean_monitor_list();
    resourceService.save(resource);

    String message = String.format(" reserved \"%s\" till %s.", resource.getName(), time2hyperlink(reserveTill));

    for (UserTimeEntry entry: resource.monitoredBy) {
      sendPersonalMessage(entry.user, user2hyperlink(user, resource.getName()) + message, turnContext).join();
    }

    return makeMentionedResponse(user, message);
  }

  private Activity getStatus(Resource resource) {
    String response;

    if (! resource.isReserved()) {
      response = String.format("Resource \"%s\" is not reserved by anyone.", resource.getName());
    }

    else {
      User reservingUser;
      try {
        reservingUser = userService.find(resource.reservedBy);
      } catch (Exception e) {
        e.printStackTrace();
        return MessageFactory.text("Exception while fetching user.");
      }
      response = String.format("Resource \"%s\" is reserved by %s till %s.", resource.getName(), user2hyperlink(reservingUser, resource.getName()), time2hyperlink(resource.reservedTill));
    }

    return MessageFactory.text(response);
  }

  private CompletableFuture<Void> sendMessage(TurnContext turnContext, String message) {
    if (message == null) {
      message = new String();
    }
    Activity replyActivity = MessageFactory.text(message);
    return sendMessage(turnContext, replyActivity);
  }

  private Activity makeMentionedResponse(TeamsChannelAccount user, String message) {
    Mention mention = mentionUser(user);
    String userName = (mention != null) ? mention.getText() : user.getName();
    Activity response = MessageFactory.text(userName + message);
    if (mention != null) {
      response.setMentions(Collections.singletonList(mention));
    }
    return response;
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

  private CompletableFuture<Void> sendPersonalMessage(String userId, String message, TurnContext turnContext) {
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

  @Override
  protected CompletableFuture<Void> onTeamsMembersAdded(
      List<TeamsChannelAccount> membersAdded,
      TeamInfo teamInfo,
      TurnContext turnContext) {
    for (TeamsChannelAccount user: membersAdded) {
      if (user.getId().equals(turnContext.getActivity().getRecipient().getId())) {
        continue;
      }
      try {
        if (!userService.exists(user)) { userService.save(new User(user)); }
      } catch (Exception e) {
        e.printStackTrace();
        System.out.println("Error while saving user");
      }
    }
    return sendMessage(turnContext, "Welcome!\nType Help for Help page.");
    // return membersAdded.stream()
    //     .filter(
    //         member -> !StringUtils
    //             .equals(member.getId(), turnContext.getActivity().getRecipient().getId()))
    //     .map(
    //         channel -> turnContext.sendActivity(MessageFactory
    //             .text("Welcome to the team " + channel.getGivenName() + " " + channel.getSurname() + ".")))
    //     .collect(CompletableFutures.toFutureList())
    //     .thenApply(resourceResponses -> null);
  }


  protected CompletableFuture<InvokeResponse> onTeamsCardActionInvoke(TurnContext turnContext) {
    System.out.println("\nKisne invoke kia\n");
    return notImplemented();
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
