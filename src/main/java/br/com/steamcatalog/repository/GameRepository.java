package br.com.steamcatalog.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import br.com.steamcatalog.model.Game;

/**
 * Repositorio Spring Data da collection "games".
 * As consultas e contagens usadas nas regras de integridade sao derivadas
 * automaticamente pelo nome do metodo.
 */
public interface GameRepository extends MongoRepository<Game, String> {

    Optional<Game> findByAppid(int appid);

    boolean existsByAppid(int appid);

    long countByDeveloperId(String developerId);

    long countByPublisherId(String publisherId);
}
