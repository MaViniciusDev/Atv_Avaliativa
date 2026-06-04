package br.com.steamcatalog.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import br.com.steamcatalog.model.Developer;

/**
 * Repositorio Spring Data da collection "developers".
 * O CRUD basico (save/findById/deleteById/count) vem do MongoRepository;
 * a consulta abaixo e derivada automaticamente pelo nome do metodo.
 */
public interface DeveloperRepository extends MongoRepository<Developer, String> {

    Optional<Developer> findByName(String name);
}
