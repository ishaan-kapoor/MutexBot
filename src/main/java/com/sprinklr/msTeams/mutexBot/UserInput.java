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

/**
 * The UserInput class handles user interactions and generates various adaptive
 * cards for the Teams bot.
 * It provides methods to generate welcome cards, resource selection cards,
 * release name selection cards, chart name selection cards, duration selection
 * cards, and admin action selection cards.
 */
@Component
public class UserInput {
  private static final String cardNotFoundMessage = "Error while loading adaptive card.<br>(CODE: json file for card not found)";
  private final ResourceService resourceService;
  private ChartNameService chartNamesService;

  /**
   * Constructs a UserInput instance with the specified services.
   *
   * @param resourceService   The service responsible for resource-related operations.
   * @param chartNamesService The service responsible for chart name operations.
   */
  @Autowired
  public UserInput(ResourceService resourceService, ChartNameService chartNamesService) {
    this.resourceService = resourceService;
    this.chartNamesService = chartNamesService;
  }

  /**
   * Generates a welcome card with options for the user.
   *
   * @return An Activity containing the welcome card.
   */
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

  /**
   * Generates a card for resource selection.
   *
   * @param turnContext The context of the current turn.
   * @return An Activity containing the resource selection card.
   */
  protected Activity resourceSelection(TurnContext turnContext) {
    return chartNameSelection();
  }

  /**
   * Generates a card for release name selection based on the given chart name.
   *
   * @param chartName The name of the chart.
   * @return An Activity containing the release name selection card.
   */
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

  /**
   * Generates a card for chart name selection.
   *
   * @return An Activity containing the chart name selection card.
   */
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

  /**
   * Generates a card for selecting the duration of an action on a resource.
   *
   * @param resource The name of the resource.
   * @param action   The action to be performed on the resource.
   * @return An Activity containing the duration selection card.
   */
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

  /**
   * Generates an adaptive card attachment from the given JSON content.
   *
   * @param content The JSON content of the adaptive card.
   * @return An Activity containing the adaptive card attachment.
   */
  protected Activity getAdaptiveCardAttachment(JsonNode content) {
    Attachment adaptiveCardAttachment = new Attachment();
    adaptiveCardAttachment.setContentType("application/vnd.microsoft.card.adaptive");
    adaptiveCardAttachment.setContent(content);
    return MessageFactory.attachment(adaptiveCardAttachment);
  }

  /**
   * Retrieves the JSON template for an adaptive card from the specified path.
   *
   * @param templatePath The path to the template JSON file.
   * @return The JSON content of the template.
   * @throws IOException If an I/O error occurs while reading the template file.
   */
  protected String getTemplateJson(String templatePath) throws IOException {
    InputStream inputStream = getClass().getResourceAsStream(templatePath);
    if (inputStream == null) { return null; }
    return IOUtils.toString(inputStream, StandardCharsets.UTF_8.toString());
  }

  /**
   * Generates a card for selecting admin actions.
   *
   * @return An Activity containing the admin action selection card.
   */
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

  /**
   * Generates a card for selecting an action to perform on a resource.
   *
   * @param resource The name of the resource to perform action on.
   * @return An Activity containing the action selection card.
   */
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
