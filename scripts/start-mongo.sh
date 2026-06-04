#!/usr/bin/env bash
# Sobe (ou reinicia) o MongoDB em um container Docker na porta 27017.
set -e

NAME="mongo-steam"

if docker ps -a --format '{{.Names}}' | grep -q "^${NAME}$"; then
    echo "Container '${NAME}' ja existe. Iniciando..."
    docker start "${NAME}"
else
    echo "Criando container '${NAME}' (MongoDB 7)..."
    docker run -d --name "${NAME}" -p 27017:27017 mongo:7
fi

echo "MongoDB disponivel em mongodb://localhost:27017"
