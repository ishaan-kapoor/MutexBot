package com.sprinklr.msTeams.mutexBot;

import com.microsoft.bot.builder.MessageFactory;
import com.microsoft.bot.builder.TurnContext;
import com.microsoft.bot.builder.teams.TeamsActivityHandler;
import com.microsoft.bot.schema.Activity;
import com.microsoft.bot.schema.teams.TeamInfo;
import com.microsoft.bot.schema.teams.TeamsChannelAccount;

import com.sprinklr.msTeams.mutexBot.model.User;
import com.sprinklr.msTeams.mutexBot.service.UserService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * The MutexBot class handles the core functionality of the Teams bot for
 * managing reservations.
 * It extends {@link TeamsActivityHandler} to provide custom behavior for
 * message and member activities.
 */
public class MutexBot extends TeamsActivityHandler {
  private final UserService userService;
  private final UserInput userInput;
  private final Actions actions;
  private final static String helpMessage = "Commands:<br> &emsp;Reserve \\<Resource\\> [for \\<Duration\\>]<br> &emsp;Release \\<Resource\\><br> &emsp;Status \\<Resource\\><br> &emsp;Monitor \\<Resource\\> [for \\<Duration\\>]<br> &emsp;StopMonitoring \\<Resource\\><br>e.g.<br> &emsp;Reserve prod:qa6 for 1h12m<br> &emsp;StopMonitoring dev:qa6<br><br>Admin only commands:<br> &emsp;Sync DB<br> &emsp;Sync Cache<br> &emsp;CreateResource \\<Resource\\><br> &emsp;DeleteResource \\<Resource\\><br> &emsp;CreateChartName \\<ChartName\\><br> &emsp;DeleteChartName \\<ChartName\\><br> &emsp;ForceRelease \\<Resource\\><br> &emsp;MakeAdmin \\<User Email\\><br> &emsp;DismissAdmin \\<User Email\\><br> &emsp;ResourceLog \\<Resource\\><br> &emsp;UserLog \\<User Email\\><br><br><hr>Send \"Hello\" for welcome card.<br>Send \"run\" to select a resource.";
  public static String URL;

  /**
   * Constructs a MutexBot instance with the specified services.
   *
   * @param userService The service responsible for user-related operations.
   * @param userInput   The class that handles user input.
   * @param actions     The class that defines actions the bot can perform.
   * @param url         The URL of the deployed bot.
   */
  @Autowired
  public MutexBot(UserService userService, UserInput userInput, Actions actions, @Value("${URL}") String url) {
    this.userService = userService;
    this.userInput = userInput;
    this.actions = actions;
    MutexBot.URL = url;
  }

  /**
   * Handles messages sent to the bot.
   *
   * <p>
   * This method processes text messages and card responses to determine the
   * appropriate action.
   * </p>
   *
   * @param turnContext The context object for this turn.
   * @return A CompletableFuture representing the async operation.
   */
  @Override
  protected CompletableFuture<Void> onMessageActivity(TurnContext turnContext) {
    turnContext.getActivity().removeRecipientMention();
    Activity response;

    // Check if the message is a card respose
    if (turnContext.getActivity().getText() == null) {
      Map<String, Object> data = (Map<String, Object>) turnContext.getActivity().getValue();
      String card = (String) data.get("card");
      if (card != null) {
        if (card.equals("durationCard")) {
          response = actions.handleDurationCard(turnContext, data);
        } else if (card.equals("adminActionsCard")) {
          response = actions.handleAdminActionsCard(turnContext, data);
        } else if (card.equals("chartNameCard")) {
          response = actions.handleChartNameCard(turnContext, data);
        } else if (card.equals("releaseNameCard")) {
          response = actions.handleReleaseNameCard(turnContext, data);
        } else {
          response = MessageFactory.text("Received unknown card: " + card);
        }
      } else {
        response = MessageFactory.text("No message recieved.");
      }
      return Utils.sendMessage(turnContext,response);
    }

    String message = turnContext.getActivity().getText().trim().replaceAll(" +", " ");
    String[] message_array = message.split(" ");
    if (message_array.length == 0) { // message can't be empty
      response = MessageFactory.text("Something went wrong.");
    } else if ((message_array.length == 1) && (message.toLowerCase().equals("help"))) {
      response = MessageFactory.text(helpMessage);
    } else if ((message_array.length == 1) && (message.toLowerCase().equals("run"))) {
      response = userInput.resourceSelection(turnContext);
    } else if ((message_array.length == 1) && (message.toLowerCase().equals("admin"))) {
      response = userInput.adminActionSelection();
    } else if ((message_array.length == 1) && (message.toLowerCase().equals("listadmins"))) {
      response = MessageFactory.text("Admins:<br><br>"+userService.listAdmins()+"<hr>");
    } else if ((message_array.length == 1) && ( (message.toLowerCase().startsWith("hello")) || (message.toLowerCase().equals("hi")) )) {
      response = userInput.welcomeCard();
    } else if (message_array.length == 2) {
      response = actions.actOnResource(turnContext, message_array[1], message_array[0].toLowerCase());
    } else if (message_array.length == 4) {
      response = actions.actOnResource(turnContext, message_array[1], message_array[0].toLowerCase(), Utils.timeString2Int(message_array[3].toLowerCase()));
    } else {
      Utils.sendMessage(turnContext, "Invalid message recieved:<br>" + message);
      response = userInput.welcomeCard();
    }
    return Utils.sendMessage(turnContext, response);
  }

  /**
   * Handles the event when members are added to a team.
   *
   * <p>
   * This method registers newly added users in the database and sends a welcome
   * message.
   * </p>
   *
   * @param membersAdded The list of members added to the team.
   * @param teamInfo     Information about the team.
   * @param turnContext  The context object for this turn.
   * @return A CompletableFuture representing the async operation.
   */
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
  }
}
