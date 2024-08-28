FROM openjdk:21-jdk-slim AS build

WORKDIR /app

# Copy Gradle wrapper and build configuration
COPY gradle /app/gradle
COPY gradlew /app/
COPY build.gradle /app/
COPY settings.gradle /app/

# Copy the source code
COPY src /app/src

#region DEBUG
RUN uname -a
RUN cat /proc/meminfo
#RUN java --version
RUN env
RUN ls -la /usr/local/openjdk-21
#endregion
# Make the Gradle wrapper executable
RUN chmod +x ./gradlew

# Build the application, excluding tests
RUN JAVA_HOME=/usr/local/openjdk-21 ./gradlew build -x test --no-daemon
RUN ./gradlew build -x test --no-daemon

# Use the same lightweight OpenJDK 21 image without unnecessary files for the final stage
FROM openjdk:21-jdk-slim

# Set the working directory inside the container
WORKDIR /app

# Copy the JAR file from the build stage
COPY --from=build /app/build/libs/xray-gatekeeper-api-0.0.1-SNAPSHOT.jar /app/xray-gatekeeper-api.jar

# Expose the port the application runs on
EXPOSE 8485

ENTRYPOINT ["java", "-jar", "/app/xray-gatekeeper-api.jar"]
