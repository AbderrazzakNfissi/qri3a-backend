FROM openjdk:21-oracle
LABEL authors="a.nfissi"
WORKDIR /app

# Copier le jar de l'application
COPY target/*.jar app.jar

# Copier le script wait-for-it.sh et rendre exécutable
COPY wait-for-it.sh wait-for-it.sh
RUN chmod +x wait-for-it.sh

EXPOSE 8080

# Utiliser wait-for-it pour attendre PostgreSQL et Elasticsearch avant de lancer l'application
ENTRYPOINT ["sh", "-c", "./wait-for-it.sh postgres:5432 -t 60 && ./wait-for-it.sh elasticsearch:9200 -t 60 && java -jar app.jar"]
