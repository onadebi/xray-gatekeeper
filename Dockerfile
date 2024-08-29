FROM eclipse-temurin:21 AS build

WORKDIR /app

#RUN apt-get update -qq && apt-get install -y --no-install-recommends -qq \
#    openssh-client \
#    curl \
#    sudo \
#    && rm -rf /var/lib/apt/lists/*

# Set JAVA_HOME environment variable
ENV JAVA_HOME=/opt/java/openjdk

# Verify JDK installation and display JAVA_HOME
RUN echo $JAVA_HOME

# Add the missing GPG key
#RUN apt-get update && apt-get install -y gnupg \
#    && apt-key adv --keyserver keyserver.ubuntu.com --recv-keys 871920D1991BC93C

#RUN apt-get update && apt install -y openjdk-21-jdk \
#    && apt-get clean


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
# RUN java -version
RUN env
RUN ls -la /opt/java/openjdk
#endregion

# Make the Gradle wrapper executable
RUN chmod +x ./gradlew

# Build the application, excluding tests
#RUN JAVA_HOME=/usr/local/openjdk-21 ./gradlew build -x test --no-daemon
RUN ./gradlew build -x test --no-daemon
#  > /dev/null 2>&1

# Use the same lightweight OpenJDK 21 image without unnecessary files for the final stage
FROM openjdk:21-jdk-slim

# Set the working directory inside the container
WORKDIR /app

# Copy the JAR file from the build stage
COPY --from=build /app/build/libs/xray-gatekeeper-api-0.0.1-SNAPSHOT.jar /app/xray-gatekeeper-api.jar

# Expose the port the application runs on
EXPOSE 8485

ENTRYPOINT ["java", "-jar", "/app/xray-gatekeeper-api.jar"]