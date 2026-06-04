package br.com.steamcatalog.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.steamcatalog.service.DeveloperService;
import br.com.steamcatalog.service.GameService;
import br.com.steamcatalog.service.PublisherService;

/** Contagem das 3 collections. */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final DeveloperService developerService;
    private final PublisherService publisherService;
    private final GameService gameService;

    public StatsController(DeveloperService developerService,
                           PublisherService publisherService,
                           GameService gameService) {
        this.developerService = developerService;
        this.publisherService = publisherService;
        this.gameService = gameService;
    }

    @GetMapping
    public Map<String, Long> stats() {
        return Map.of(
                "developers", developerService.count(),
                "publishers", publisherService.count(),
                "games", gameService.count());
    }
}
