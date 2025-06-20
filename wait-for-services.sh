#!/bin/bash
set -e

echo "Waiting for PostgreSQL..."
until PGPASSWORD=mysecretpassword psql -h postgres -U qri3a5432 -d qri3adb -c "SELECT 1" > /dev/null 2>&1; do
  echo "PostgreSQL is unavailable - sleeping"
  sleep 2
done
echo "PostgreSQL is up"


#echo "Elasticsearch is up"

echo "Starting the application..."
exec java -jar app.jar