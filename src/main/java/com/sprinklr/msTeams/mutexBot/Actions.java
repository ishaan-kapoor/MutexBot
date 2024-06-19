package com.sprinklr.msTeams.mutexBot;

import com.microsoft.bot.builder.MessageFactory;
import com.microsoft.bot.builder.TurnContext;
import com.microsoft.bot.builder.teams.TeamsInfo;
import com.microsoft.bot.schema.Activity;
import com.microsoft.bot.schema.teams.TeamsChannelAccount;
import com.sprinklr.msTeams.mutexBot.model.Resource;
import com.sprinklr.msTeams.mutexBot.model.User;
import com.sprinklr.msTeams.mutexBot.service.ChartNameService;
import com.sprinklr.msTeams.mutexBot.service.MonitorLogService;
import com.sprinklr.msTeams.mutexBot.service.ReservationLogService;
import com.sprinklr.msTeams.mutexBot.service.ResourceService;
import com.sprinklr.msTeams.mutexBot.service.UserService;
import com.sprinklr.msTeams.mutexBot.utils.UserTimeEntry;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Actions {
  private ResourceService resourceService;
  private UserService userService;
  private UserInput userInput;
  private String appId;
  private String appPassword;
  private ReservationLogService reservationLogService;
  private MonitorLogService monitorLogService;
  private ChartNameService chartNameService;
  private final static Object mutex = new Object();

  public Actions(ResourceService resourceService, UserService userService, ReservationLogService reservationLogService, MonitorLogService monitorLogService, ChartNameService chartNameService, UserInput userInput,
      @Value("${MicrosoftAppId}") String appId, @Value("${MicrosoftAppPassword}") String appPassword) {
    this.resourceService = resourceService;
    this.userService = userService;
    this.userInput = userInput;
    this.reservationLogService = reservationLogService;
    this.monitorLogService = monitorLogService;
    this.chartNameService = chartNameService;
    this.appId = appId;
    this.appPassword = appPassword;
  }

  protected CompletableFuture<Void> actOnResource(TurnContext turnContext, String resource_name) {
    Resource resource;
    try {
      resource = resourceService.find(resource_name);
    } catch (Exception e) {
      e.printStackTrace();
      return Utils.sendMessage(turnContext, "Exception while fetching resource.");
    }
    if (resource == null) {
      return Utils.sendMessage(turnContext, "Resource \"" + resource_name + "\" not found.");
    }
    return actOnResource(turnContext, resource_name, "*");
  }

  protected CompletableFuture<Void> actOnResource(TurnContext turnContext, String resource_name, String action) {
    for (String act : Utils.adminActions) {
      if (act.toLowerCase().equals(action)) {
        return adminAction(turnContext, resource_name, action);
      }
    }

    if (action.equals("*")) {
      return userInput.actionSelection(turnContext, resource_name);
    } else if ((action.equals("reserve")) || action.equals("monitor")) {
      return userInput.durationSelection(turnContext, resource_name, action);
    }

    TeamsChannelAccount user = TeamsInfo.getMember(turnContext, turnContext.getActivity().getFrom().getId()).join();
    if (!userService.exists(user)) {
      userService.save(new User(user));
    }

    Activity response;
    synchronized (mutex) {
      Resource resource;
      try {
        resource = resourceService.find(resource_name);
      } catch (Exception e) {
        e.printStackTrace();
        return Utils.sendMessage(turnContext, "Exception while fetching resource.");
      }
      if (resource == null) {
        return Utils.sendMessage(turnContext, "Resource \"" + resource_name + "\" not found.");
      }

      if (action.equals("release")) {
        response = releaseResource(user, turnContext, resource);
      } else if (action.equals("stopmonitoring")) {
        response = stopMonitoringResource(user, resource);
      } else if (action.equals("status")) {
        response = getStatus(resource);
      } else {
        response = MessageFactory.text(String.format(Utils.UNSURE_ACTION_MESSAGE, resource_name, action));
      }
    }
    return Utils.sendMessage(turnContext, response);
  }

  protected CompletableFuture<Void> adminAction(TurnContext turnContext, String resource_name, String action) {
    String user_id = turnContext.getActivity().getFrom().getId();
    TeamsChannelAccount teamsUser = TeamsInfo.getMember(turnContext, user_id).join();
    User user;
    try {
      user = userService.find(user_id);
    } catch (Exception e) {
      e.printStackTrace();
      return Utils.sendMessage(turnContext, "Exception while fetching user.");
    }
    if ((user == null)) {
      userService.save(new User(teamsUser));
      return Utils.sendMessage(turnContext, "Only admins can perform this action");
    }
    if (!userService.exists(user_id)) {
      userService.save(new User(teamsUser));
    }
    if (!user.isAdmin()) {
      return Utils.sendMessage(turnContext, "Only admins can perform this action");
    }

    User argUser = userService.findByEmail(resource_name);

    if (action.equals("makeadmin")) {
      if (argUser == null) {
        return Utils.sendMessage(turnContext, "User not found");
      }
      if (argUser.isAdmin()) {
        return Utils.sendMessage(turnContext, String.format("%s is already admin.", Utils.user2hyperlink(argUser)));
      }
      argUser.makeAdmin();
      userService.save(argUser);
      return Utils.sendMessage(turnContext, String.format("%s is now an admin", Utils.user2hyperlink(argUser)));
    }

    if (action.equals("dismissadmin")) {
      if (argUser == null) {
        return Utils.sendMessage(turnContext, "User not found");
      }
      if (!argUser.isAdmin()) {
        return Utils.sendMessage(turnContext, String.format("%s was not admin.", Utils.user2hyperlink(argUser)));
      }
      argUser.dismissAdmin();
      userService.save(argUser);
      return Utils.sendMessage(turnContext, String.format("%s is now dismissed as admin", Utils.user2hyperlink(argUser)));
    }

    if (action.equals("userlog")) {
      if (argUser == null) {
        return Utils.sendMessage(turnContext, "User not found");
      }
      return Utils.sendMessage(turnContext, "[report]("+Utils.URL+"logs/?perspective=user&user="+argUser.getId()+")");
    }

    boolean exists = chartNameService.exists(resource_name);
    if (action.equals("createchartname")) {
      if (exists) {
        return Utils.sendMessage(turnContext, "Chart name \"" + resource_name + "\" already exists.");
      }
      chartNameService.save(resource_name);
      return Utils.sendMessage(turnContext, "Chart name \"" + resource_name + "\" created successfully.");
    }

    if (action.equals("deletechartname")) {
      if (!exists) {
        return Utils.sendMessage(turnContext, "Chart name \"" + resource_name + "\" not found.");
      }
      chartNameService.delete(resource_name);
      return Utils.sendMessage(turnContext, "Chart name \"" + resource_name + "\" deleted successfully.");
    }

    synchronized (mutex) {
      exists = resourceService.exists(resource_name);
      if (action.equals("createresource")) {
        if (exists) {
          return Utils.sendMessage(turnContext, "Resource \"" + resource_name + "\" already exists.");
        }
        resourceService.save(new Resource(resource_name));
        return Utils.sendMessage(turnContext, "Resource \"" + resource_name + "\" created successfully.");
      }

      if (action.equals("deleteresource")) {
        if (!exists) {
          return Utils.sendMessage(turnContext, "Resource \"" + resource_name + "\" not found.");
        }
        resourceService.delete(resource_name);
        return Utils.sendMessage(turnContext, "Resource \"" + resource_name + "\" deleted successfully.");
      }

      if (action.equals("forcerelease")) {
        Resource resource;
        try {
          resource = resourceService.find(resource_name);
        } catch (Exception e) {
          e.printStackTrace();
          return Utils.sendMessage(turnContext, "Exception while fetching resource.");
        }
        if (resource == null) {
          return Utils.sendMessage(turnContext, "Resource \"" + resource_name + "\" not found.");
        }
        return Utils.sendMessage(turnContext, releaseResource(teamsUser, turnContext, resource, true));
      }
    }

    if (action.equals("resourcelog")) {
      if (!exists) {
        return Utils.sendMessage(turnContext, "Resource \"" + resource_name + "\" not found.");
      }
      return Utils.sendMessage(turnContext, "[report]("+Utils.URL+"logs/?perspective=resource&resource="+resource_name+")");
    }

    return Utils.sendMessage(turnContext, "Invalid admin action: " + action);
  }

  protected CompletableFuture<Void> actOnResource(TurnContext turnContext, String resource_name, String action,
      Integer duration) {
    TeamsChannelAccount user = TeamsInfo.getMember(turnContext, turnContext.getActivity().getFrom().getId()).join();
    if (!userService.exists(user)) {
      userService.save(new User(user));
    }

    if (duration <= 0) {
      return Utils.sendMessage(turnContext, MessageFactory.text("Duration can't be -ve or zero"));
    }

    Activity response;
    synchronized (mutex) {
      Resource resource;
      try {
        resource = resourceService.find(resource_name);
      } catch (Exception e) {
        e.printStackTrace();
        return Utils.sendMessage(turnContext, "Exception while fetching resource.");
      }
      if (resource == null) {
        return Utils.sendMessage(turnContext, "Resource \"" + resource_name + "\" not found.");
      }

      if (action.equals("reserve")) {
        response = reserveResource(user, turnContext, resource, duration);
      } else if (action.equals("monitor")) {
        response = monitorResource(user, resource, duration);
      } else {
        response = MessageFactory.text(String.format(Utils.UNSURE_ACTION_MESSAGE, resource_name, action));
      }
    }
    return Utils.sendMessage(turnContext, response);
  }

  protected Activity stopMonitoringResource(TeamsChannelAccount user, Resource resource) {
    resource.stopMonitoring(user.getId());
    resourceService.save(resource);

    monitorLogService.stopMonitoring(resource.getName(), user.getId());

    String message = String.format(" stopped monitoring \"%s\".", resource.getName());
    return Utils.makeMentionedResponse(user, message);
  }

  protected Activity monitorResource(TeamsChannelAccount user, Resource resource, int duration) {
    LocalDateTime monitorTill = LocalDateTime.now().plusMinutes(duration);
    resource.monitor(user.getId(), monitorTill);
    resourceService.save(resource);

    monitorLogService.monitor(resource.getName(), user.getId(), monitorTill);

    String message = String.format(" is monitoring \"%s\" till %s.", resource.getName(),
        Utils.time2hyperlink(monitorTill));
    return Utils.makeMentionedResponse(user, message);
  }

  protected Activity releaseResource(TeamsChannelAccount user, TurnContext turnContext, Resource resource) {
    return releaseResource(user, turnContext, resource, false);
  }
  protected Activity releaseResource(TeamsChannelAccount user, TurnContext turnContext, Resource resource, boolean force) {
    if (!resource.isReserved()) {
      return MessageFactory.text(String.format("Resource \"%s\" is not reserved by anyone.", resource.getName()));
    }

    String reservingUserId = resource.reservedBy;
    if (!user.getId().equals(reservingUserId)) {
      User releasingUser = null;
      if (force) {
        try {
          releasingUser = userService.find(user.getId());
        } catch (Exception e) {
          e.printStackTrace();
          return MessageFactory.text("Error while fetching releasing user");
        }
        if (releasingUser == null) {
          releasingUser = new User(user);
          userService.save(releasingUser);
        }
      }
      if (!(force && releasingUser.isAdmin())) {
        User reservingUser;
        try {
          reservingUser = userService.find(reservingUserId);
        } catch (Exception e) {
          System.out.println("Error while fetching reserving user");
          e.printStackTrace();
          reservingUser = new User(reservingUserId);
        }
        return MessageFactory.text(String.format("Resource \"%s\" is reserved by %s till %s", resource.getName(),
            Utils.user2hyperlink(reservingUser, resource.getName()), Utils.time2hyperlink(resource.reservedTill)));
      }
    }

    resource.release();
    resource.clean_monitor_list();
    resourceService.save(resource);

    reservationLogService.release(resource.getName(), resource.reservedBy);

    String message = String.format(" released \"%s\".", resource.getName());

    for (UserTimeEntry entry : resource.monitoredBy) {
      Utils.sendPersonalMessage(entry.user, Utils.user2hyperlink(user, resource.getName()) + message, turnContext,
          appId, appPassword).join();
    }

    return Utils.makeMentionedResponse(user, message);
  }

  protected Activity reserveResource(TeamsChannelAccount user, TurnContext turnContext, Resource resource,
      int duration) {
    if (resource.isReserved()) {
      User reservingUser;
      try {
        reservingUser = userService.find(resource.reservedBy);
      } catch (Exception e) {
        e.printStackTrace();
        return MessageFactory.text("Exception while fetching user.");
      }
      return MessageFactory.text(String.format("Resource \"%s\" is already reserved by %s till %s.", resource.getName(),
          Utils.user2hyperlink(reservingUser, resource.getName()), Utils.time2hyperlink(resource.reservedTill)));
    }

    if (resource.maxAllocationTime < duration) {
      return MessageFactory.text(String.format("Duration can't be more than %d hours and %d minutes.", resource.maxAllocationTime / 60, resource.maxAllocationTime % 60));
    }

    LocalDateTime reserveTill = LocalDateTime.now().plusMinutes(duration);
    resource.reserve(user.getId(), reserveTill);
    resource.clean_monitor_list();
    resourceService.save(resource);

    reservationLogService.reserve(resource.getName(), user.getId(), reserveTill);

    String message = String.format(" reserved \"%s\" till %s.", resource.getName(), Utils.time2hyperlink(reserveTill));

    for (UserTimeEntry entry : resource.monitoredBy) {
      Utils.sendPersonalMessage(entry.user, Utils.user2hyperlink(user, resource.getName()) + message, turnContext,
          appId, appPassword).join();
    }

    return Utils.makeMentionedResponse(user, message);
  }

  protected Activity getStatus(Resource resource) {
    String response;

    if (!resource.isReserved()) {
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
      response = String.format("Resource \"%s\" is reserved by %s till %s.", resource.getName(),
          Utils.user2hyperlink(reservingUser, resource.getName()), Utils.time2hyperlink(resource.reservedTill));
    }

    return MessageFactory.text(response);
  }

  protected CompletableFuture<Void> handleAdminActionsCard(TurnContext turnContext, Map<String, Object> data) {
    String action = (String) data.get("action");
    String arg = (String) data.get("arg");
    if ((action == null)||(arg == null)) {
      return Utils.sendMessage(turnContext, "Please enter a valid action and arg.");
    }
    return adminAction(turnContext, arg, action);
  }

  protected CompletableFuture<Void> handleReleaseNameCard(TurnContext turnContext, Map<String, Object> data) {
    String releaseName = (String) data.get("value");
    if (releaseName == null) {
      return Utils.sendMessage(turnContext, "Please enter a valid resource name.");
    }
    return actOnResource(turnContext, releaseName);
  }

  protected CompletableFuture<Void> handleChartNameCard(TurnContext turnContext, Map<String, Object> data) {
    String chartName = (String) data.get("value");
    if (chartName == null) {
      return Utils.sendMessage(turnContext, "Please enter a valid resource name.");
    }
    return userInput.releaseNameSelection(turnContext, chartName);
  }

  protected CompletableFuture<Void> handleResourceFormCard(TurnContext turnContext, Map<String, Object> data) {
    String chartName = (String) data.get("chart_name");
    String releaseName = (String) data.get("chart_release_name");
    if ((chartName == null) || (releaseName == null)) {
      return Utils.sendMessage(turnContext, "Please enter a valid resource name.");
    }
    return actOnResource(turnContext, chartName + "-" + releaseName);
  }

  protected CompletableFuture<Void> handleResourceCard(TurnContext turnContext, Map<String, Object> data) {
    String resource = (String) data.get("resource");
    if (resource == null) {
      return Utils.sendMessage(turnContext, "Please enter a valid resource name.");
    }
    return actOnResource(turnContext, resource);
  }

  protected CompletableFuture<Void> handleDurationCard(TurnContext turnContext, Map<String, Object> data) {
    Integer hours = Integer.parseInt((String) data.get("hours"));
    Integer minutes = Integer.parseInt((String) data.get("minutes"));
    String resource = (String) data.get("resource");
    String action = (String) data.get("action");

    if (hours != null && minutes != null && resource != null && action != null) {
      return actOnResource(turnContext, resource, action, hours * 60 + minutes);
    } else {
      return Utils.sendMessage(turnContext, "Please enter valid values.");
    }
  }

}
