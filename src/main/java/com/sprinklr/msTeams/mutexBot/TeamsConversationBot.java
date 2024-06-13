package com.sprinklr.msTeams.mutexBot;

import com.microsoft.bot.builder.TurnContext;
import com.microsoft.bot.builder.teams.TeamsActivityHandler;
import com.microsoft.bot.schema.teams.TeamInfo;
import com.microsoft.bot.schema.teams.TeamsChannelAccount;

import com.sprinklr.msTeams.mutexBot.model.User;
import com.sprinklr.msTeams.mutexBot.service.ResourceService;
import com.sprinklr.msTeams.mutexBot.service.UserService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

public class TeamsConversationBot extends TeamsActivityHandler {
  private final String appId;
  private final String appPassword;
  private final ResourceService resourceService;
  private final UserService userService;
  private final UserInput userInput;
  private final Actions actions;
  private final static String helpMessage = "Commands:<br> &emsp;Reserve \\<Resource\\> [for \\<Duration\\>]<br> &emsp;Release \\<Resource\\><br> &emsp;Status \\<Resource\\><br> &emsp;Monitor \\<Resource\\> [for \\<Duration\\>]<br> &emsp;StopMonitoring \\<Resource\\><br>e.g.<br> &emsp;Reserve prod:qa6 for 1h12m<br> &emsp;StopMonitoring dev:qa6<br><br>Admin only commands:<br> &emsp;CreateResource \\<Resource\\><br> &emsp;DeleteResource \\<Resource\\><br> &emsp;ForceRelease \\<Resource\\><br> &emsp;MakeAdmin \\<user email\\><br> &emsp;DismissAdmin \\<user email\\><br><br><hr>Send \"Hello\" for welcome card.<br>Send \"run\" to select a resource.";

  @Autowired
  public TeamsConversationBot(
      @Value("${MicrosoftAppId}") String appId,
      @Value("${MicrosoftAppPassword}") String appPassword,
      ResourceService resourceService,
      UserService userService,
      UserInput userInput,
      Actions actions) {
    this.appId = appId;
    this.appPassword = appPassword;
    this.resourceService = resourceService;
    this.userService = userService;
    this.userInput = userInput;
    this.actions = actions;
  }

  @Override
  protected CompletableFuture<Void> onMessageActivity(TurnContext turnContext) {
    turnContext.getActivity().removeRecipientMention();

    // Check if the message is a card respose
    if (turnContext.getActivity().getText() == null) {
      Map<String, Object> data = (Map<String, Object>) turnContext.getActivity().getValue();
      String card = (String) data.get("card");
      if (card != null) {
        if (card.equals("durationCard")) {
          return actions.handleDurationCard(turnContext, data);
        } else if (card.equals("resourceCard")) {
          return actions.handleResourceCard(turnContext, data);
        } else if (card.equals("adminActionsCard")) {
          return actions.handleAdminActionsCard(turnContext, data);
        }
      }
      return Utils.sendMessage(turnContext, "No message recieved.");
    }

    String message = turnContext.getActivity().getText().trim().replaceAll(" +", " ");
    String[] message_array = message.split(" ");
    if (message_array.length == 0) {
      return Utils.sendMessage(turnContext, "Something went wrong.");
    } else if ((message_array.length == 1) && (message.toLowerCase().equals("help"))) {
      return Utils.sendMessage(turnContext, helpMessage);
    } else if ((message_array.length == 1) && (message.toLowerCase().equals("run"))) {
      return userInput.resourceSelection(turnContext);
    } else if ((message_array.length == 1) && (message.toLowerCase().equals("admin"))) {
      return userInput.adminActionSelection(turnContext);
    } else if ((message_array.length == 1) && ( (message.toLowerCase().equals("hello")) || (message.toLowerCase().equals("hi")) )) {
      return Utils.sendMessage(turnContext, userInput.welcomeCard());
    } else if (message_array.length == 2) {
      return actions.actOnResource(turnContext, message_array[1], message_array[0].toLowerCase());
    } else if (message_array.length == 4) {
      return actions.actOnResource(turnContext, message_array[1], message_array[0].toLowerCase(),
          Utils.timeString2Int(message_array[3].toLowerCase()));
    }
    Utils.sendMessage(turnContext, "Invalid message recieved:<br>" + message);
    return Utils.sendMessage(turnContext, userInput.welcomeCard());

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

  @Override
  protected CompletableFuture<Void> onTeamsMembersAdded(
      List<TeamsChannelAccount> membersAdded,
      TeamInfo teamInfo,
      TurnContext turnContext) {

    // Register newly added users to DB
    String bot_id = turnContext.getActivity().getRecipient().getId();
    for (TeamsChannelAccount user : membersAdded) {
      if (user.getId().equals(bot_id)) { continue; }
      if (!userService.exists(user)) { userService.save(new User(user)); }
    }

    return Utils.sendMessage(turnContext, userInput.welcomeCard());
    // return membersAdded.stream()
    // .filter(
    // member -> !StringUtils
    // .equals(member.getId(), turnContext.getActivity().getRecipient().getId()))
    // .map(
    // channel -> turnContext.sendActivity(MessageFactory
    // .text("Welcome to the team " + channel.getGivenName() + " " +
    // channel.getSurname() + ".")))
    // .collect(CompletableFutures.toFutureList())
    // .thenApply(resourceResponses -> null);
  }
}
