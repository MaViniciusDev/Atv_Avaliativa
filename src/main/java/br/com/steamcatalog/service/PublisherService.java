package br.com.steamcatalog.service;

import java.util.List;
import java.util.regex.Pattern;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import br.com.steamcatalog.exception.IntegrityException;
import br.com.steamcatalog.exception.NotFoundException;
import br.com.steamcatalog.exception.ValidationException;
import br.com.steamcatalog.model.Publisher;
import br.com.steamcatalog.repository.GameRepository;
import br.com.steamcatalog.repository.PublisherRepository;

/**
 * Regras de negocio da collection Publisher (equivalente a "Aluno").
 *  - Cadastro/alteracao validam dados e unicidade do nome;
 *  - Exclusao bloqueada se houver Game vinculado (integridade referencial).
 */
@Service
public class PublisherService {

    private final PublisherRepository repository;
    private final GameRepository gameRepository;
    private final MongoTemplate mongoTemplate;

    public PublisherService(PublisherRepository repository,
                            GameRepository gameRepository,
                            MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.gameRepository = gameRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public Publisher create(String name, String country) {
        String cleanName = requireName(name);
        if (repository.findByName(cleanName).isPresent()) {
            throw new ValidationException("Ja existe um publisher com o nome '" + cleanName + "'.");
        }
        return repository.save(new Publisher(cleanName, blankToNull(country)));
    }

    public Publisher update(String id, String name, String country) {
        Publisher existing = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Publisher nao encontrado para o id informado."));
        String cleanName = requireName(name);
        repository.findByName(cleanName).ifPresent(other -> {
            if (!other.getId().equals(id)) {
                throw new ValidationException("Ja existe outro publisher com o nome '" + cleanName + "'.");
            }
        });
        existing.setName(cleanName);
        existing.setCountry(blankToNull(country));
        return repository.save(existing);
    }

    public void delete(String id) {
        Publisher existing = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Publisher nao encontrado para o id informado."));
        long vinculos = gameRepository.countByPublisherId(id);
        if (vinculos > 0) {
            throw new IntegrityException("Nao e possivel excluir o publisher '" + existing.getName()
                    + "': existem " + vinculos + " jogo(s) vinculado(s) a ele.");
        }
        repository.deleteById(id);
    }

    public Publisher findById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Publisher nao encontrado para o id informado."));
    }

    public Publisher findByName(String name) {
        return repository.findByName(requireName(name)).orElse(null);
    }

    public List<Publisher> search(String namePart, String country, int limit) {
        Query query = new Query();
        if (namePart != null && !namePart.isBlank()) {
            query.addCriteria(Criteria.where("name").regex(Pattern.quote(namePart.trim()), "i"));
        }
        if (country != null && !country.isBlank()) {
            query.addCriteria(Criteria.where("country").regex(Pattern.quote(country.trim()), "i"));
        }
        query.limit(limit);
        return mongoTemplate.find(query, Publisher.class);
    }

    public long count() {
        return repository.count();
    }

    private String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("O nome do publisher e obrigatorio.");
        }
        String clean = name.trim();
        if (clean.length() > 200) {
            throw new ValidationException("O nome do publisher e muito longo (max 200).");
        }
        return clean;
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
