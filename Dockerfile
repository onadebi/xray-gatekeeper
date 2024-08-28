FROM gradle:8.4-jdk21 AS build

WORKDIR /app
# Explicitly setting JAVA_HOME environment variable, due to possible invalid setting in gradle image
ENV JAVA_HOME=/opt/java/openjdk

# Copy Gradle wrapper and build configuration
COPY gradle /app/gradle
COPY gradlew /app/
COPY build.gradle /app/
COPY settings.gradle /app/

# Copy the source code
COPY src /app/src

# Set executable permissions for gradlew
RUN chmod +x gradlew

#debug purpose:
RUN whereis java
RUN gradle build -x test
#RUN ./gradlew build -x test

# Usage of lightweight JDK image for running the application, so as to make the container lighter
FROM openjdk:21-jdk-slim

# Set the working directory inside the container
WORKDIR /app

# Copy the JAR file from the build stage
COPY --from=build /app/build/libs/xray-gatekeeper-api-0.0.1-SNAPSHOT.jar /app/xray-gatekeeper-api.jar

# Expose the port the application runs on
EXPOSE 8485

CMD ["java", "-jar", "/app/xray-gatekeeper-api.jar"]
