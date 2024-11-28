# Use an official OpenJDK runtime as a parent image
FROM openjdk:21-oracle
LABEL authors="a.nfissi"

# Set the working directory inside the container
WORKDIR /app

# Copy the jar file from the host to the container
# Replace 'target/your-app.jar' with the path to your built jar
COPY target/*.jar app.jar

# Expose the port the application runs on
# Replace 8080 with your application's port if different
EXPOSE 8080

# Define the entry point to run the jar
ENTRYPOINT ["java", "-jar","app.jar"]

