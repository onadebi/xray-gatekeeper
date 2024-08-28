FROM openjdk:21-jdk-slim AS build

# Install Gradle
RUN apt-get update && \
    apt-get install -y --no-install-recommends gnupg wget unzip && \
    apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys 0E98404D386FA1D9 6ED0E7B82643E131 F8D2585B8783D481 54404762BBB6E853 BDE6D2B9216EC7A8 && \
    wget https://services.gradle.org/distributions/gradle-8.4-bin.zip -P /tmp && \
    unzip /tmp/gradle-8.4-bin.zip -d /opt && \
    ln -s /opt/gradle-8.4/bin/gradle /usr/bin/gradle && \
    apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

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