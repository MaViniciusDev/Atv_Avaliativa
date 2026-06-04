package br.com.steamcatalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada da aplicacao Spring Boot.
 *
 * Sobe o servidor web embutido (Tomcat) que serve o front-end (em
 * src/main/resources/static) e a API REST. Abra http://localhost:8080.
 *
 * Rodar com:
 *   ./mvnw spring-boot:run
 *   ou: java -jar target/steam-catalog.jar
 */
@SpringBootApplication
public class SteamCatalogApplication {

    public static void main(String[] args) {
        SpringApplication.run(SteamCatalogApplication.class, args);
    }
}
