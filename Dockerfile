FROM openjdk:21-oracle
LABEL authors="a.nfissi"
WORKDIR /app

# Installation de curl et PostgreSQL client pour les health checks
RUN microdnf install -y curl postgresql

# Copier le jar de l'application
COPY target/*.jar app.jar

# Ajouter un script de démarrage
COPY wait-for-services.sh .
RUN chmod +x wait-for-services.sh

EXPOSE 8081

# Utiliser le script pour attendre que les services soient disponibles
ENTRYPOINT ["./wait-for-services.sh"]