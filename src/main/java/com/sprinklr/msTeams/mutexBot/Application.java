package com.sprinklr.msTeams.mutexBot;

import com.microsoft.bot.integration.AdapterWithErrorHandler;
import com.microsoft.bot.integration.BotFrameworkHttpAdapter;
import com.microsoft.bot.integration.Configuration;
import com.microsoft.bot.integration.spring.BotController;
import com.microsoft.bot.integration.spring.BotDependencyConfiguration;

import com.sprinklr.msTeams.mutexBot.service.UserService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the Mutex Bot application.
 * 
 * <p>
 * This class extends {@link BotDependencyConfiguration}, which provides the
 * default
 * implementations for a Bot application. It overrides methods to provide custom
 * implementations specific to the Mutex Bot.
 * </p>
 */

@SpringBootApplication
@EnableScheduling
@EnableAsync
@Import({ BotController.class })
public class Application extends BotDependencyConfiguration {
  /**
   * @param args for command line arguments
   */
  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  /**
   * Returns the Bot implementation for this application.
   * 
   * <p>
   * The @Component annotation could be used on the Bot class instead of this
   * method
   * with the @Bean annotation.
   * </p>
   *
   * @param userService The service responsible for user-related operations.
   * @param userInput   The class that handles user input.
   * @param actions     The class that defines actions the bot can perform.
   * @param url         The URL of the deployed bot.
   * @return An instance of {@link MutexBot}.
   */
  @Bean
  public MutexBot getBot(UserService userService, UserInput userInput, Actions actions,  @Value("${URL}") String url) {
    return new MutexBot(userService, userInput, actions, url);
  }

  /**
   * Returns a custom Adapter that provides error handling.
   *
   * @param configuration The Configuration object to use.
   * @return An instance of {@link AdapterWithErrorHandler}.
   */
  @Override
  public BotFrameworkHttpAdapter getBotFrameworkHttpAdaptor(Configuration configuration) {
    return new AdapterWithErrorHandler(configuration);
  }
}
