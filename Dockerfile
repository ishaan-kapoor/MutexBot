# Use an official OpenJDK runtime as a parent image
FROM openjdk:17-jdk-slim

# Set the working directory in the container
WORKDIR /app

# Copy the application JAR file into the container
COPY target/MutexBot-1.0.jar app.jar

# Expose the port your application runs on
EXPOSE 3978

# Set the environment variables if necessary
ENV GL="glpat-ySE_axhY1EmDgiFMn4sh"
ENV DB="mongodb+srv://admin:learning@testcluster.9xdkqg5.mongodb.net/?retryWrites=true&w=majority&appName=TestCluster"

# Run the JAR file
ENTRYPOINT ["java","-jar","app.jar"]

