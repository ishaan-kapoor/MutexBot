package com.sprinklr.msTeams.mutexBot;

import com.microsoft.bot.integration.AdapterWithErrorHandler;
import com.microsoft.bot.integration.BotFrameworkHttpAdapter;
import com.microsoft.bot.integration.Configuration;
import com.microsoft.bot.integration.spring.BotController;
import com.microsoft.bot.integration.spring.BotDependencyConfiguration;

import com.sprinklr.msTeams.mutexBot.service.UserService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@Import({ BotController.class })

/**
 * This class extends the BotDependencyConfiguration which provides the default
 * implementations for a Bot application. The Application class should
 * override methods in order to provide custom implementations.
 */
public class Application extends BotDependencyConfiguration {
  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  /**
   * Returns the Bot for this application.
   *
   * <p>
   * The @Component annotation could be used on the Bot class instead of this
   * method
   * with the @Bean annotation.
   * </p>
   *
   * @return The Bot implementation for this application.
   */
  @Bean
  public MutexBot getBot(UserService userService, UserInput userInput, Actions actions) {
    return new MutexBot(userService, userInput, actions);
  }

  /**
   * Returns a custom Adapter that provides error handling.
   *
   * @param configuration The Configuration object to use.
   * @return An error handling BotFrameworkHttpAdapter.
   */
  @Override
  public BotFrameworkHttpAdapter getBotFrameworkHttpAdaptor(Configuration configuration) {
    return new AdapterWithErrorHandler(configuration);
  }
}
