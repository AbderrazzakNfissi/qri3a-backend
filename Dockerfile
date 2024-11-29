FROM openjdk:21-oracle
LABEL authors="a.nfissi"
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar","app.jar"]

