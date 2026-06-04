package br.com.steamcatalog.service;

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import br.com.steamcatalog.model.Developer;
import br.com.steamcatalog.model.Game;
import br.com.steamcatalog.model.Publisher;
import br.com.steamcatalog.repository.DeveloperRepository;
import br.com.steamcatalog.repository.GameRepository;
import br.com.steamcatalog.repository.PublisherRepository;
import br.com.steamcatalog.util.CsvReader;

/**
 * Servico de importacao do CSV do dataset Steam Games.
 *
 * REQUISITO: a importacao roda em uma THREAD em segundo plano, de modo que a
 * aplicacao permaneca responsiva e o usuario possa dispara-la a qualquer momento.
 *
 * Estrategia:
 *  - Uma thread "coordenadora" le o CSV sequencialmente (necessario porque um
 *    registro pode ocupar varias linhas) e resolve Developer/Publisher,
 *    garantindo que existam ANTES do jogo (integridade);
 *  - Os INSERTs dos jogos sao distribuidos para um pool de threads.
 */
@Service
public class ImportService {

    // Indices das colunas no arquivo SteamGames_cleaned.csv
    private static final int COL_APPID = 0;
    private static final int COL_NAME = 1;
    private static final int COL_TYPE = 2;
    private static final int COL_RELEASE = 3;
    private static final int COL_DEVELOPERS = 4;
    private static final int COL_PUBLISHERS = 5;
    private static final int COL_DESCRIPTION = 6;
    private static final int COL_PRICE = 7;
    private static final int COL_THUMBNAIL = 8;
    private static final int COL_REVIEWSCORE = 9;
    private static final int COL_POSITIVE = 10;
    private static final int COL_NEGATIVE = 11;
    private static final int COL_RANK = 14;
    private static final int COL_TAGS = 18;

    private static final int WORKER_THREADS = 4;
    private static final String DEFAULT_CSV = "data/SteamGames_cleaned.csv";

    private final DeveloperRepository developerRepository;
    private final PublisherRepository publisherRepository;
    private final GameRepository gameRepository;

    // caches nome -> id para evitar consultas repetidas durante a carga
    private final ConcurrentHashMap<String, String> developerCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> publisherCache = new ConcurrentHashMap<>();

    private final ImportProgress progress = new ImportProgress();

    public ImportService(DeveloperRepository developerRepository,
                         PublisherRepository publisherRepository,
                         GameRepository gameRepository) {
        this.developerRepository = developerRepository;
        this.publisherRepository = publisherRepository;
        this.gameRepository = gameRepository;
    }

    public ImportProgress getProgress() {
        return progress;
    }

    public boolean isRunning() {
        return progress.isRunning();
    }

    /**
     * Dispara a importacao em uma thread separada e retorna imediatamente.
     *
     * @param csvPath caminho do arquivo CSV (vazio = caminho padrao)
     * @param limit   numero maximo de jogos a importar (0 = todos)
     * @return true se a importacao foi iniciada; false se ja havia uma rodando.
     */
    public boolean startImport(String csvPath, int limit) {
        if (!progress.start()) {
            return false; // ja existe uma importacao em andamento
        }
        String path = (csvPath == null || csvPath.isBlank()) ? DEFAULT_CSV : csvPath;
        Thread worker = new Thread(() -> runImport(path, limit), "csv-import-thread");
        worker.setDaemon(true);
        worker.start();
        return true;
    }

    private void runImport(String csvPath, int limit) {
        Path path = Path.of(csvPath);
        if (!Files.exists(path)) {
            progress.finish("ERRO: arquivo nao encontrado: " + csvPath);
            return;
        }
        preloadCaches();

        ExecutorService pool = Executors.newFixedThreadPool(WORKER_THREADS);
        try (CsvReader csv = new CsvReader(new FileReader(path.toFile(), StandardCharsets.UTF_8))) {
            csv.readRecord(); // descarta o cabecalho

            List<String> record;
            int imported = 0;
            while ((record = csv.readRecord()) != null) {
                if (record.isEmpty() || record.size() < COL_TAGS + 1) {
                    continue;
                }
                progress.incRead();
                try {
                    Game game = buildGame(record);
                    if (game == null) {
                        progress.incSkipped();
                        continue;
                    }
                    game.setDeveloperId(resolveDeveloper(firstToken(record.get(COL_DEVELOPERS))));
                    game.setPublisherId(resolvePublisher(firstToken(record.get(COL_PUBLISHERS))));

                    pool.submit(() -> insertGame(game));

                    imported++;
                    if (limit > 0 && imported >= limit) {
                        break;
                    }
                } catch (Exception e) {
                    progress.incSkipped();
                }
            }

            pool.shutdown();
            pool.awaitTermination(10, TimeUnit.MINUTES);
            progress.finish("Importacao concluida com sucesso.");
        } catch (IOException e) {
            pool.shutdownNow();
            progress.finish("ERRO de leitura do CSV: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            progress.finish("Importacao interrompida.");
        } catch (Exception e) {
            pool.shutdownNow();
            progress.finish("ERRO inesperado: " + e.getMessage());
        }
    }

    /**
     * Recarrega os caches a partir do estado ATUAL do banco. Limpa antes para
     * nao reaproveitar ids de uma carga anterior (importante se a base tiver
     * sido esvaziada entre importacoes).
     */
    private void preloadCaches() {
        developerCache.clear();
        publisherCache.clear();
        for (Developer d : developerRepository.findAll()) {
            if (d.getName() != null) {
                developerCache.put(d.getName(), d.getId());
            }
        }
        for (Publisher p : publisherRepository.findAll()) {
            if (p.getName() != null) {
                publisherCache.put(p.getName(), p.getId());
            }
        }
    }

    private Game buildGame(List<String> row) {
        int appid = parseInt(row.get(COL_APPID), -1);
        String name = safe(row.get(COL_NAME));
        if (appid <= 0 || name.isBlank()) {
            return null;
        }
        Game game = new Game();
        game.setAppid(appid);
        game.setName(name);
        String type = safe(row.get(COL_TYPE));
        game.setType(type.isBlank() ? "game" : type.toLowerCase());
        game.setReleaseDate(normalizeDate(row.get(COL_RELEASE)));
        game.setPrice(parseDouble(row.get(COL_PRICE), 0.0));
        game.setDescription(truncate(safe(row.get(COL_DESCRIPTION)), 500));
        String thumb = safe(row.get(COL_THUMBNAIL));
        game.setThumbnail(thumb.startsWith("http") ? thumb : null);
        game.setReviewScore(clamp(parseInt(row.get(COL_REVIEWSCORE), 0), 0, 10));
        game.setPositiveReview(Math.max(0, parseInt(row.get(COL_POSITIVE), 0)));
        game.setNegativeReview(Math.max(0, parseInt(row.get(COL_NEGATIVE), 0)));
        game.setRank(Math.max(0, parseInt(row.get(COL_RANK), 0)));
        game.setTags(parseTags(row.get(COL_TAGS)));
        return game;
    }

    private void insertGame(Game game) {
        try {
            gameRepository.save(game);
            progress.incInserted();
        } catch (DuplicateKeyException e) {
            progress.incSkipped(); // appid duplicado (indice unico)
        } catch (Exception e) {
            progress.incSkipped();
        }
    }

    private String resolveDeveloper(String name) {
        String key = (name == null || name.isBlank()) ? "Desconhecido" : name.trim();
        return developerCache.computeIfAbsent(key, k -> {
            try {
                Developer dev = developerRepository.save(new Developer(k));
                progress.incDevelopersCreated();
                return dev.getId();
            } catch (DuplicateKeyException dup) {
                return developerRepository.findByName(k).map(Developer::getId).orElse(null);
            }
        });
    }

    private String resolvePublisher(String name) {
        String key = (name == null || name.isBlank()) ? "Desconhecido" : name.trim();
        return publisherCache.computeIfAbsent(key, k -> {
            try {
                Publisher pub = publisherRepository.save(new Publisher(k));
                progress.incPublishersCreated();
                return pub.getId();
            } catch (DuplicateKeyException dup) {
                return publisherRepository.findByName(k).map(Publisher::getId).orElse(null);
            }
        });
    }

    // ----- helpers de parsing -----

    private static String firstToken(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim();
        int comma = v.indexOf(',');
        return (comma >= 0) ? v.substring(0, comma).trim() : v;
    }

    private static List<String> parseTags(String value) {
        List<String> tags = new ArrayList<>();
        if (value == null || value.isBlank()) {
            return tags;
        }
        for (String t : value.split(",")) {
            String tag = t.trim().toLowerCase();
            if (!tag.isEmpty()) {
                tags.add(tag);
            }
        }
        return tags;
    }

    private static String normalizeDate(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim();
        return v.matches("\\d{4}-\\d{2}-\\d{2}") ? v : null;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static int parseInt(String value, int def) {
        if (value == null || value.isBlank()) {
            return def;
        }
        try {
            return (int) Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static double parseDouble(String value, double def) {
        if (value == null || value.isBlank()) {
            return def;
        }
        try {
            return Double.parseDouble(value.trim().replace("$", ""));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
