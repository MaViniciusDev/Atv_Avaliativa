package br.com.steamcatalog.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.steamcatalog.service.ImportProgress;
import br.com.steamcatalog.service.ImportService;

/** Dispara e acompanha a importacao do CSV (executada em thread). */
@RestController
@RequestMapping("/api/import")
public class ImportController {

    private final ImportService importService;

    public ImportController(ImportService importService) {
        this.importService = importService;
    }

    /** Inicia a importacao em segundo plano. */
    @PostMapping
    public ResponseEntity<Map<String, Object>> start(@RequestBody(required = false) ImportRequest body) {
        String path = (body == null) ? null : body.path();
        int limit = (body == null || body.limit() == null) ? 0 : body.limit();
        boolean started = importService.startImport(path, limit);
        return ResponseEntity
                .status(started ? HttpStatus.ACCEPTED : HttpStatus.CONFLICT)
                .body(Map.of("started", started));
    }

    /** Progresso atual da importacao. */
    @GetMapping("/status")
    public Map<String, Object> status() {
        ImportProgress p = importService.getProgress();
        return Map.of(
                "running", p.isRunning(),
                "message", p.getLastMessage(),
                "totalRead", p.getTotalRead(),
                "inserted", p.getInserted(),
                "skipped", p.getSkipped(),
                "developersCreated", p.getDevelopersCreated(),
                "publishersCreated", p.getPublishersCreated());
    }

    public record ImportRequest(String path, Integer limit) {
    }
}
