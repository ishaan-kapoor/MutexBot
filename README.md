# MutexBot

The Resource Reservation Bot for Microsoft Teams is a comprehensive solution aimed at simplifying and streamlining the management of critical resources within teams and organizations. This bot allows users to reserve resources such as servers for exclusive access, ensuring smooth deployments and efficient utilization of resources. Once done, users can release these resources for others to use, promoting a culture of sharing and cooperation.
One of the key features of this bot is its ability to prevent booking conflicts. By keeping track of all reservations, the bot ensures there are no overlaps, leading to smoother operations and less confusion.
In addition, the bot enhances collaboration by managing all bookings within Microsoft Teams. This visibility allows team members to stay informed about resource usage and plan their work accordingly.
To further aid in planning, the bot also sends out notifications about upcoming bookings and potential conflicts. This proactive approach helps teams to effectively plan ahead, avoid last-minute surprises, and ensure uninterrupted work.
Overall, the Resource Reservation Bot for Microsoft Teams is a powerful tool designed to foster collaboration, improve resource management, and enhance operational efficiency in a seamless and user-friendly manner.

### Note:
All secrets like database uri, gitlab tokens etc, have been revoked.


## Table of Contents

- [Installation](#installation)
- [Usage](#usage)
- [Endpoints](#endpoints)
- [Testing](#testing)
- [Configuration](#configuration)
- [Contributing](#contributing)

## Installation

1. Clone the repository:
```bash
git clone https://github.com/ishaan-kapoor/MutexBot.git
cd MutexBot
```
2. Run the script
```bash
./run
```

OR

2.1. Package the application
```bash
mvn clean package
```
2.2. Run the application:
```bash
java -jar ./target/MutexBot-1.0.jar
```

## Usage
To use the bot, you need to configure it with your Microsoft Teams App ID and Password. The bot interacts with users through Microsoft Teams, allowing them to perform various actions on resources.

## Endpoints
### Log Calendar
- Get user/resource logs: `/logs`
    - Parameters:
        - `resource`: (Optional) Name of the resource
        - `user`: (Optional) ID of the user
        - `perspective`: (Optional) Perspective of the Logs (resource or user)
    - Example
      ```curl
      GET /logs?resource=testResource&user=testUser&perspective=resource
      ```
- Get documentation of each package, class and method: `/docs/index.html`
    - Example
      ```curl
      GET /docs/index.html
      ```

## Testing
The project includes unit tests for various components. To run the tests, use the following command:
```bash
mvn test
```

## Configuration
The application requires configuration for Microsoft Teams integration. Add the following properties to your `application.properties` file:
```properties
URL=url-where-the-bot-is-deployed
spring.data.mongodb.uri=uri-of-the-mongo-db-database
gitlab.token=gitlab-token-with-access-to-helm-charts
```

## Contributing
Contributions are welcome! Please follow these steps to contribute:

1. Fork the repository.
2. Create a new branch.
3. Make your changes.
4. Submit a pull request.
Please ensure your code includes appropriate tests.

