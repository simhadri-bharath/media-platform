# Use official OpenJDK 17 slim image
FROM openjdk:17-jdk-slim

# Set working directory
WORKDIR /app

# Copy Maven build output (JAR)
COPY target/media-backend-0.0.1-SNAPSHOT.jar app.jar

# Expose application port
EXPOSE 8080

# Run the JAR
ENTRYPOINT ["java","-jar","app.jar"]

