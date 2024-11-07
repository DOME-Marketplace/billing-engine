FROM openjdk:17-jdk-alpine

COPY ./target/billing-engine*.jar /usr/app/billing-engine.jar

RUN apt-get update && apt-get install -y \
	curl \
	wget

ENTRYPOINT exec java $JAVA_OPTS -jar billing-engine.jar