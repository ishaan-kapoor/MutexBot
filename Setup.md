### Prerequisites:
- Java
- Maven
- ngrok
- Azure bot resource: [Tutorial](https://learn.microsoft.com/en-us/microsoftteams/platform/sbs-teams-conversation-bot?tabs=ngrok)
### Steps:
1. clone the repo.
2. run the `ngrok_proxy` script and note the url.
3. Change the Messaging endpoint (change it to the url you got from previous step) in Configuration settings of Bot Resource from [Azure Portal](https://portal.azure.com/)
4. Make required changes in `./src/main/resources/application.properties`
5. Change the environment variable declarations in the `run` script and execute it.
6. Open the `teamsAppManifest` directory and compress its contents in an archive.
7. Load this archive as a bot in MSTeams.

