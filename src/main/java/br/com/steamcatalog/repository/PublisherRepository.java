package br.com.steamcatalog.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import br.com.steamcatalog.model.Publisher;

/**
 * Repositorio Spring Data da collection "publishers".
 */
public interface PublisherRepository extends MongoRepository<Publisher, String> {

    Optional<Publisher> findByName(String name);
}
