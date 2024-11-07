FROM openjdk:17-jdk-alpine

COPY ./target/billing-engine*.jar /usr/app/billing-engine.jar

ENTRYPOINT exec java $JAVA_OPTS -jar billing-engine.jar