# ============================================================================
# Build multi-stage da aplicacao Spring Boot.
#  Etapa 1: compila o JAR com Maven.
#  Etapa 2: imagem enxuta apenas com o JRE para executar.
# ============================================================================

# ---- etapa de build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
# baixa as dependencias primeiro (melhora o cache de camadas)
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
# compila e empacota (sem testes; os testes precisam de um MongoDB ativo)
COPY src ./src
RUN mvn -q -B clean package -DskipTests

# ---- etapa de runtime ----
FROM eclipse-temurin:21-jre
WORKDIR /app
# o JAR executavel do Spring Boot
COPY --from=build /build/target/steam-catalog.jar app.jar
# o dataset (a importacao le data/SteamGames_cleaned.csv relativo ao workdir)
COPY data ./data
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]
