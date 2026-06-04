package br.com.steamcatalog.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import br.com.steamcatalog.exception.IntegrityException;
import br.com.steamcatalog.exception.NotFoundException;
import br.com.steamcatalog.exception.ValidationException;
import br.com.steamcatalog.model.Developer;
import br.com.steamcatalog.model.Game;
import br.com.steamcatalog.model.Publisher;
import br.com.steamcatalog.repository.DeveloperRepository;
import br.com.steamcatalog.repository.GameRepository;
import br.com.steamcatalog.repository.PublisherRepository;

/**
 * Regras de negocio da collection Game (equivalente a "Turma").
 *
 * REGRA DE INTEGRIDADE PRINCIPAL: um Game so pode ser cadastrado/alterado se o
 * Developer e o Publisher referenciados JA existirem em suas collections.
 */
@Service
public class GameService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

    private final GameRepository repository;
    private final DeveloperRepository developerRepository;
    private final PublisherRepository publisherRepository;
    private final MongoTemplate mongoTemplate;

    public GameService(GameRepository repository,
                       DeveloperRepository developerRepository,
                       PublisherRepository publisherRepository,
                       MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.developerRepository = developerRepository;
        this.publisherRepository = publisherRepository;
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Cadastra um jogo individualmente recebendo os NOMES do developer e do
     * publisher. Ambos precisam existir previamente (senao IntegrityException).
     */
    public Game create(Game game, String developerName, String publisherName) {
        validate(game);
        Developer dev = requireDeveloper(developerName);
        Publisher pub = requirePublisher(publisherName);

        if (repository.findByAppid(game.getAppid()).isPresent()) {
            throw new ValidationException("Ja existe um jogo com appid " + game.getAppid() + ".");
        }
        game.setDeveloperId(dev.getId());
        game.setPublisherId(pub.getId());
        return repository.save(game);
    }

    /** Altera os dados de um jogo (inclusive trocando developer/publisher). */
    public Game update(int appid, Game changes, String developerName, String publisherName) {
        Game existing = repository.findByAppid(appid)
                .orElseThrow(() -> new NotFoundException("Jogo nao encontrado para o appid " + appid + "."));
        changes.setAppid(appid);
        changes.setId(existing.getId());
        validate(changes);

        Developer dev = requireDeveloper(developerName);
        Publisher pub = requirePublisher(publisherName);
        changes.setDeveloperId(dev.getId());
        changes.setPublisherId(pub.getId());
        return repository.save(changes);
    }

    /** Exclui um jogo pelo appid. */
    public void delete(int appid) {
        Game existing = repository.findByAppid(appid)
                .orElseThrow(() -> new NotFoundException("Jogo nao encontrado para o appid " + appid + "."));
        repository.deleteById(existing.getId());
    }

    /** Localiza um jogo individualmente pelo appid. */
    public Game findByAppid(int appid) {
        return repository.findByAppid(appid)
                .orElseThrow(() -> new NotFoundException("Jogo nao encontrado para o appid " + appid + "."));
    }

    /**
     * Pesquisa por MULTIPLOS criterios combinados em AND. Developer/publisher
     * sao informados por NOME e resolvidos aqui para o respectivo id.
     */
    public List<Game> search(GameSearchCriteria c) {
        Query query = new Query();

        if (notBlank(c.getName())) {
            query.addCriteria(Criteria.where("name").regex(Pattern.quote(c.getName().trim()), "i"));
        }
        if (notBlank(c.getType())) {
            query.addCriteria(Criteria.where("type").is(c.getType().trim().toLowerCase()));
        }
        // preco: combina min/max num UNICO criterio (o Spring Data nao aceita
        // dois criterios separados sobre o mesmo campo "price").
        if (c.getMinPrice() != null || c.getMaxPrice() != null) {
            Criteria price = Criteria.where("price");
            if (c.getMinPrice() != null) {
                price = price.gte(c.getMinPrice());
            }
            if (c.getMaxPrice() != null) {
                price = price.lte(c.getMaxPrice());
            }
            query.addCriteria(price);
        }
        if (c.getMinScore() != null) {
            query.addCriteria(Criteria.where("reviewScore").gte(c.getMinScore()));
        }
        if (c.getMaxRank() != null) {
            query.addCriteria(Criteria.where("rank").lte(c.getMaxRank()));
        }
        if (notBlank(c.getTag())) {
            query.addCriteria(Criteria.where("tags").is(c.getTag().trim().toLowerCase()));
        }
        if (notBlank(c.getDeveloperName())) {
            Developer dev = developerRepository.findByName(c.getDeveloperName().trim()).orElse(null);
            if (dev == null) {
                return new ArrayList<>();
            }
            query.addCriteria(Criteria.where("developerId").is(dev.getId()));
        }
        if (notBlank(c.getPublisherName())) {
            Publisher pub = publisherRepository.findByName(c.getPublisherName().trim()).orElse(null);
            if (pub == null) {
                return new ArrayList<>();
            }
            query.addCriteria(Criteria.where("publisherId").is(pub.getId()));
        }

        query.with(Sort.by(Sort.Direction.ASC, "rank")).limit(c.getLimit());
        return mongoTemplate.find(query, Game.class);
    }

    public long count() {
        return repository.count();
    }

    /** Resolve o nome do developer do jogo (para exibicao). */
    public String resolveDeveloperName(Game game) {
        if (game.getDeveloperId() == null) {
            return "(sem developer)";
        }
        return developerRepository.findById(game.getDeveloperId())
                .map(Developer::getName).orElse("(developer removido)");
    }

    public String resolvePublisherName(Game game) {
        if (game.getPublisherId() == null) {
            return "(sem publisher)";
        }
        return publisherRepository.findById(game.getPublisherId())
                .map(Publisher::getName).orElse("(publisher removido)");
    }

    // ----- validacoes -----

    private Developer requireDeveloper(String name) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("O developer do jogo e obrigatorio.");
        }
        return developerRepository.findByName(name.trim())
                .orElseThrow(() -> new IntegrityException("Developer '" + name.trim()
                        + "' nao cadastrado. Cadastre-o antes de criar o jogo."));
    }

    private Publisher requirePublisher(String name) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("O publisher do jogo e obrigatorio.");
        }
        return publisherRepository.findByName(name.trim())
                .orElseThrow(() -> new IntegrityException("Publisher '" + name.trim()
                        + "' nao cadastrado. Cadastre-o antes de criar o jogo."));
    }

    private void validate(Game game) {
        if (game == null) {
            throw new ValidationException("Dados do jogo ausentes.");
        }
        if (game.getAppid() <= 0) {
            throw new ValidationException("O appid deve ser um inteiro positivo.");
        }
        if (game.getName() == null || game.getName().isBlank()) {
            throw new ValidationException("O nome do jogo e obrigatorio.");
        }
        if (game.getType() == null || game.getType().isBlank()) {
            throw new ValidationException("O tipo do jogo e obrigatorio (ex: game, dlc).");
        }
        if (game.getPrice() < 0) {
            throw new ValidationException("O preco nao pode ser negativo.");
        }
        if (game.getReviewScore() < 0 || game.getReviewScore() > 10) {
            throw new ValidationException("O reviewScore deve estar entre 0 e 10.");
        }
        if (game.getPositiveReview() < 0 || game.getNegativeReview() < 0) {
            throw new ValidationException("Quantidades de reviews nao podem ser negativas.");
        }
        if (game.getRank() < 0) {
            throw new ValidationException("O rank nao pode ser negativo.");
        }
        if (game.getReleaseDate() != null && !game.getReleaseDate().isBlank()) {
            try {
                LocalDate.parse(game.getReleaseDate().trim(), ISO);
            } catch (DateTimeParseException e) {
                throw new ValidationException("Data de lancamento invalida (use o formato yyyy-MM-dd).");
            }
        }
        game.setType(game.getType().trim().toLowerCase());
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
