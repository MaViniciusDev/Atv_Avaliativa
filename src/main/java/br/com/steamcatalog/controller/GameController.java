package br.com.steamcatalog.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.steamcatalog.dto.GameRequest;
import br.com.steamcatalog.dto.GameResponse;
import br.com.steamcatalog.dto.ListResponse;
import br.com.steamcatalog.model.Game;
import br.com.steamcatalog.service.GameSearchCriteria;
import br.com.steamcatalog.service.GameService;

/** CRUD e pesquisa (multiplos criterios) de jogos. */
@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameService service;

    public GameController(GameService service) {
        this.service = service;
    }

    /** Pesquisa por multiplos criterios combinados em AND. */
    @GetMapping
    public ListResponse search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Integer minScore,
            @RequestParam(required = false) Integer maxRank,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String developer,
            @RequestParam(required = false) String publisher,
            @RequestParam(defaultValue = "24") int limit) {

        GameSearchCriteria c = new GameSearchCriteria()
                .name(name).type(type)
                .minPrice(minPrice).maxPrice(maxPrice)
                .minScore(minScore).maxRank(maxRank)
                .tag(tag).developerName(developer).publisherName(publisher)
                .limit(limit);

        List<GameResponse> items = service.search(c).stream().map(this::toResponse).toList();
        return ListResponse.of(items);
    }

    @GetMapping("/{appid}")
    public GameResponse byAppid(@PathVariable int appid) {
        return toResponse(service.findByAppid(appid));
    }

    @PostMapping
    public ResponseEntity<GameResponse> create(@RequestBody GameRequest body) {
        Game created = service.create(body.toGame(), body.developer(), body.publisher());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @PutMapping("/{appid}")
    public GameResponse update(@PathVariable int appid, @RequestBody GameRequest body) {
        Game updated = service.update(appid, body.toGame(), body.developer(), body.publisher());
        return toResponse(updated);
    }

    @DeleteMapping("/{appid}")
    public Map<String, Boolean> delete(@PathVariable int appid) {
        service.delete(appid);
        return Map.of("deleted", true);
    }

    private GameResponse toResponse(Game g) {
        return GameResponse.of(g, service.resolveDeveloperName(g), service.resolvePublisherName(g));
    }
}
