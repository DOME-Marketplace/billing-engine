
# Use a JDK17 as set in the dev environment
FROM openjdk:17-jdk-alpine 

# If there si the need to upgrade to a newer version of Java use the following FROM
# FROM alpine/java:21-jdk

# Install curl
RUN apk update && apk add --no-cache curl

# Set the workdir in the container
WORKDIR /usr/app

# Copy JAR in the working directory
COPY target/billing-engine.jar billing-engine.jar

# Espose port 8080
EXPOSE 8080

# Comand to run the Spring Boot application
ENTRYPOINT ["java","-jar","billing-engine.jar"]