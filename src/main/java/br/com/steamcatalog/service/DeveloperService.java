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
import br.com.steamcatalog.model.Developer;
import br.com.steamcatalog.repository.DeveloperRepository;
import br.com.steamcatalog.repository.GameRepository;

/**
 * Regras de negocio da collection Developer (equivalente a "Professor").
 *  - Cadastro/alteracao validam dados e unicidade do nome;
 *  - Exclusao bloqueada se houver Game vinculado (integridade referencial).
 */
@Service
public class DeveloperService {

    private final DeveloperRepository repository;
    private final GameRepository gameRepository;
    private final MongoTemplate mongoTemplate;

    public DeveloperService(DeveloperRepository repository,
                            GameRepository gameRepository,
                            MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.gameRepository = gameRepository;
        this.mongoTemplate = mongoTemplate;
    }

    /** Cadastra um developer individualmente. */
    public Developer create(String name, String country) {
        String cleanName = requireName(name);
        if (repository.findByName(cleanName).isPresent()) {
            throw new ValidationException("Ja existe um developer com o nome '" + cleanName + "'.");
        }
        return repository.save(new Developer(cleanName, blankToNull(country)));
    }

    /** Altera um developer existente. */
    public Developer update(String id, String name, String country) {
        Developer existing = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Developer nao encontrado para o id informado."));
        String cleanName = requireName(name);
        repository.findByName(cleanName).ifPresent(other -> {
            if (!other.getId().equals(id)) {
                throw new ValidationException("Ja existe outro developer com o nome '" + cleanName + "'.");
            }
        });
        existing.setName(cleanName);
        existing.setCountry(blankToNull(country));
        return repository.save(existing);
    }

    /** Exclui um developer, respeitando a integridade referencial. */
    public void delete(String id) {
        Developer existing = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Developer nao encontrado para o id informado."));
        long vinculos = gameRepository.countByDeveloperId(id);
        if (vinculos > 0) {
            throw new IntegrityException("Nao e possivel excluir o developer '" + existing.getName()
                    + "': existem " + vinculos + " jogo(s) vinculado(s) a ele.");
        }
        repository.deleteById(id);
    }

    /** Localiza um developer individualmente pelo id. */
    public Developer findById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Developer nao encontrado para o id informado."));
    }

    /** Localiza um developer pelo nome exato (ou null). */
    public Developer findByName(String name) {
        return repository.findByName(requireName(name)).orElse(null);
    }

    /**
     * Pesquisa por multiplos criterios (todos opcionais, combinados em AND):
     * parte do nome (case-insensitive) e pais.
     */
    public List<Developer> search(String namePart, String country, int limit) {
        Query query = new Query();
        if (namePart != null && !namePart.isBlank()) {
            query.addCriteria(Criteria.where("name").regex(Pattern.quote(namePart.trim()), "i"));
        }
        if (country != null && !country.isBlank()) {
            query.addCriteria(Criteria.where("country").regex(Pattern.quote(country.trim()), "i"));
        }
        query.limit(limit);
        return mongoTemplate.find(query, Developer.class);
    }

    public long count() {
        return repository.count();
    }

    // ----- validacoes auxiliares -----

    private String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("O nome do developer e obrigatorio.");
        }
        String clean = name.trim();
        if (clean.length() > 200) {
            throw new ValidationException("O nome do developer e muito longo (max 200).");
        }
        return clean;
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
