package com.sprinklr.msTeams.mutexBot;

import com.fasterxml.jackson.databind.JsonNode;
import com.microsoft.bot.builder.MessageFactory;
import com.microsoft.bot.builder.TurnContext;
import com.microsoft.bot.schema.ActionTypes;
import com.microsoft.bot.schema.Activity;
import com.microsoft.bot.schema.Attachment;
import com.microsoft.bot.schema.CardAction;
import com.microsoft.bot.schema.HeroCard;
import com.microsoft.bot.schema.Serialization;

import com.sprinklr.msTeams.mutexBot.service.ChartNameService;
import com.sprinklr.msTeams.mutexBot.service.ResourceService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserInput {
  private static final String cardNotFoundMessage = "Error while loading adaptive card.<br>(CODE: json file for card not found)";
  private final ResourceService resourceService;
  private ChartNameService chartNamesService;

  @Autowired
  public UserInput(ResourceService resourceService, ChartNameService chartNamesService) {
    this.resourceService = resourceService;
    this.chartNamesService = chartNamesService;
  }

  protected Activity welcomeCard() {
    List<CardAction> buttons = new ArrayList<>();

    CardAction helpButton = new CardAction();
    helpButton.setType(ActionTypes.MESSAGE_BACK);
    helpButton.setTitle("Refer to the Command Manual");
    helpButton.setText("help");
    buttons.add(helpButton);

    CardAction runButton = new CardAction();
    runButton.setType(ActionTypes.MESSAGE_BACK);
    runButton.setTitle("Select a Resource");
    runButton.setText("run");
    buttons.add(runButton);

    CardAction adminButton = new CardAction();
    adminButton.setType(ActionTypes.MESSAGE_BACK);
    adminButton.setTitle("Perform Admin actions");
    adminButton.setText("admin");
    buttons.add(adminButton);

    CardAction listAdminButton = new CardAction();
    listAdminButton.setType(ActionTypes.MESSAGE_BACK);
    listAdminButton.setTitle("List all Admins");
    listAdminButton.setText("listAdmins");
    buttons.add(listAdminButton);

    HeroCard card = new HeroCard();
    card.setButtons(buttons);
    card.setTitle("Welcome!");
    card.setSubtitle("Manage your Jenkins Resources with Mutex Bot.");
    card.setText("Once reserved, no one can override your builds, till you release the resource.");
    return MessageFactory.attachment(card.toAttachment());
  }

  protected Activity resourceSelection(TurnContext turnContext) {
    return chartNameSelection();
  }

  protected Activity releaseNameSelection(String chartName) {
    String templateJSON;
    try {
      templateJSON = getTemplateJson(Utils.RESOURCE_ADAPTIVE_CARD_TEMPLATE);
    } catch (IOException e) {
      e.printStackTrace();
      return MessageFactory.text("Error while loading adaptive card.<br>" + e);
    }
    if (templateJSON == null) { return MessageFactory.text(cardNotFoundMessage); }

    StringBuilder chartChoicesBuilder = new StringBuilder();
    List<String> resourceNames = resourceService.findByChartName(chartName);
    String releaseName;
    for (String resourceName : resourceNames) {
      if (chartChoicesBuilder.length() > 0) { chartChoicesBuilder.append(", "); }
      releaseName = String.format(
          "{\"title\": \"%s\", \"value\": \"%s\"}",
          resourceName.substring(chartName.length() + 1), resourceName
      );
      chartChoicesBuilder.append(releaseName);
    }
    String cardJSON = templateJSON.replaceFirst("\\{\\}", chartChoicesBuilder.toString());
    cardJSON = cardJSON.replace("$(fieldName)", "Release Name (for '" + chartName + "'):");
    cardJSON = cardJSON.replace("$(cardName)", "releaseNameCard");

    JsonNode content;
    try {
      content = Serialization.jsonToTree(cardJSON);
    } catch (IOException e) {
      e.printStackTrace();
      return MessageFactory.text("Error while serializing adaptive card.<br>" + e);
    }

    return getAdaptiveCardAttachment(content);
  }

  protected Activity chartNameSelection() {
    String templateJSON;
    try {
      templateJSON = getTemplateJson(Utils.RESOURCE_ADAPTIVE_CARD_TEMPLATE);
    } catch (IOException e) {
      e.printStackTrace();
      return MessageFactory.text("Error while loading adaptive card.<br>" + e);
    }
    if (templateJSON == null) { return MessageFactory.text(cardNotFoundMessage); }

    StringBuilder chartChoicesBuilder = new StringBuilder();
    List<String> chart_names = chartNamesService.getAll();
    for (String chart_name : chart_names) {
      if (chartChoicesBuilder.length() > 0) { chartChoicesBuilder.append(", "); }
      chartChoicesBuilder.append(String.format("{\"title\": \"%s\", \"value\": \"%s\"}", chart_name, chart_name));
    }
    String cardJSON = templateJSON.replaceFirst("\\{\\}", chartChoicesBuilder.toString());
    cardJSON = cardJSON.replace("$(fieldName)", "Chart Name:");
    cardJSON = cardJSON.replace("$(cardName)", "chartNameCard");

    JsonNode content;
    try {
      content = Serialization.jsonToTree(cardJSON);
    } catch (IOException e) {
      e.printStackTrace();
      return MessageFactory.text("Error while serializing adaptive card.<br>" + e);
    }

    return getAdaptiveCardAttachment(content);
  }

  protected Activity durationSelection(String resource, String action) {

    String templateJSON;
    try {
      templateJSON = getTemplateJson(Utils.DURATION_ADAPTIVE_CARD_TEMPLATE);
    } catch (IOException e) {
      e.printStackTrace();
      return MessageFactory.text("Error while loading adaptive card.<br>" + e);
    }
    if (templateJSON == null) { return MessageFactory.text(cardNotFoundMessage); }

    String cardJSON = templateJSON.replaceAll("\\$\\{resource\\}", resource).replaceAll("\\$\\{action\\}", action);
    JsonNode content;
    try {
      content = Serialization.jsonToTree(cardJSON);
    } catch (IOException e) {
      e.printStackTrace();
      return MessageFactory.text("Error while serializing adaptive card.<br>" + e);
    }

    return getAdaptiveCardAttachment(content);
  }

  protected Activity getAdaptiveCardAttachment(JsonNode content) {
    Attachment adaptiveCardAttachment = new Attachment();
    adaptiveCardAttachment.setContentType("application/vnd.microsoft.card.adaptive");
    adaptiveCardAttachment.setContent(content);
    return MessageFactory.attachment(adaptiveCardAttachment);
  }

  protected String getTemplateJson(String templatePath) throws IOException {
    InputStream inputStream = getClass().getResourceAsStream(templatePath);
    if (inputStream == null) { return null; }
    return IOUtils.toString(inputStream, StandardCharsets.UTF_8.toString());
  }

  protected Activity adminActionSelection() {
    String templateJSON;
    try {
      templateJSON = getTemplateJson(Utils.ADMIN_ACTIONS_ADAPTIVE_CARD_TEMPLATE);
    } catch (IOException e) {
      e.printStackTrace();
      return MessageFactory.text("Error while loading adaptive card.<br>" + e);
    }
    if (templateJSON == null) { return MessageFactory.text(cardNotFoundMessage); }

    StringBuilder choicesBuilder = new StringBuilder();
    for (String action : Utils.adminActions) {
      if (choicesBuilder.length() > 0) { choicesBuilder.append(", "); }
      choicesBuilder.append(String.format("{\"title\": \"%s\", \"value\": \"%s\"}", action, action.toLowerCase()));
    }
    String cardJSON = templateJSON.replace("{}", choicesBuilder.toString());

    JsonNode content;
    try {
      content = Serialization.jsonToTree(cardJSON);
    } catch (IOException e) {
      e.printStackTrace();
      return MessageFactory.text("Error while serializing adaptive card.<br>" + e);
    }

    return getAdaptiveCardAttachment(content);
  }

  protected Activity actionSelection(String resource) {
    List<CardAction> buttons = new ArrayList<>();
    for (String action : Utils.actions) {
      CardAction cardAction = new CardAction();
      cardAction.setType(ActionTypes.MESSAGE_BACK);
      cardAction.setTitle(action);
      cardAction.setText(action + " " + resource);
      buttons.add(cardAction);
    }
    HeroCard card = new HeroCard();
    card.setButtons(buttons);
    card.setTitle("Choose the action to perform on \"" + resource + "\".");
    return MessageFactory.attachment(card.toAttachment());
  }
}
