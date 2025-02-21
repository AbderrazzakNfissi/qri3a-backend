FROM openjdk:21-oracle
LABEL authors="a.nfissi"
WORKDIR /app

# Copier le jar de l'application
COPY target/*.jar app.jar

EXPOSE 8080

# Attendre que PostgreSQL et Elasticsearch soient disponibles, puis lancer l'application
ENTRYPOINT ["sh", "-c", "until echo > /dev/tcp/postgres/5432; do echo 'Waiting for PostgreSQL'; sleep 1; done; until echo > /dev/tcp/elasticsearch/9200; do echo 'Waiting for Elasticsearch'; sleep 1; done; java -jar app.jar"]
