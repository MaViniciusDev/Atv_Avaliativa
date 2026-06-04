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

import br.com.steamcatalog.dto.EntityRequest;
import br.com.steamcatalog.dto.EntityResponse;
import br.com.steamcatalog.dto.ListResponse;
import br.com.steamcatalog.service.PublisherService;

/** CRUD e pesquisa de publishers. */
@RestController
@RequestMapping("/api/publishers")
public class PublisherController {

    private final PublisherService service;

    public PublisherController(PublisherService service) {
        this.service = service;
    }

    @GetMapping
    public ListResponse search(@RequestParam(required = false) String name,
                               @RequestParam(required = false) String country,
                               @RequestParam(defaultValue = "50") int limit) {
        List<EntityResponse> items = service.search(name, country, limit)
                .stream().map(EntityResponse::of).toList();
        return ListResponse.of(items);
    }

    @PostMapping
    public ResponseEntity<EntityResponse> create(@RequestBody EntityRequest body) {
        EntityResponse resp = EntityResponse.of(service.create(body.name(), body.country()));
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @PutMapping("/{id}")
    public EntityResponse update(@PathVariable String id, @RequestBody EntityRequest body) {
        return EntityResponse.of(service.update(id, body.name(), body.country()));
    }

    @DeleteMapping("/{id}")
    public Map<String, Boolean> delete(@PathVariable String id) {
        service.delete(id);
        return Map.of("deleted", true);
    }
}
