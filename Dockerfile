FROM openjdk:21-jdk-slim AS build

# Install Gradle
RUN apt-get update && \
    apt-get install -y wget unzip && \
    wget https://services.gradle.org/distributions/gradle-8.4-bin.zip -P /tmp && \
    unzip /tmp/gradle-8.4-bin.zip -d /opt && \
    ln -s /opt/gradle-8.4/bin/gradle /usr/bin/gradle

WORKDIR /app

# Copy Gradle wrapper and build configuration
COPY gradle /app/gradle
COPY gradlew /app/
COPY build.gradle /app/
COPY settings.gradle /app/

# Copy the source code
COPY src /app/src

# Set executable permissions for gradlew to prevent remote build error
RUN chmod +x gradlew

# Run Gradle build
RUN ./gradlew build -x test

# Usage of lightweight JDK image for running the application, so as to make the container lighter
FROM openjdk:21-jdk-slim

WORKDIR /app

# Copy the JAR file from the build stage
COPY --from=build /app/build/libs/xray-gatekeeper-api-0.0.1-SNAPSHOT.jar /app/xray-gatekeeper-api.jar

EXPOSE 8485

CMD ["java", "-jar", "/app/xray-gatekeeper-api.jar"]