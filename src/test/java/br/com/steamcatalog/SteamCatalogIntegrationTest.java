package br.com.steamcatalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import br.com.steamcatalog.model.Game;
import br.com.steamcatalog.service.GameSearchCriteria;
import br.com.steamcatalog.service.GameService;
import br.com.steamcatalog.service.ImportService;
import br.com.steamcatalog.repository.DeveloperRepository;
import br.com.steamcatalog.repository.GameRepository;
import br.com.steamcatalog.repository.PublisherRepository;

/**
 * Teste de integracao que sobe o contexto Spring Boot real (servidor web em
 * porta aleatoria) e exercita TODOS os requisitos do enunciado de ponta a ponta:
 *  - importacao via thread;
 *  - CRUD individual nas 3 collections (via API REST);
 *  - pesquisas com multiplos criterios;
 *  - regras de integridade referencial (status 409) e validacao (status 400).
 *
 * Usa um banco de teste separado (steamcatalog_test), limpo a cada execucao.
 * Requer um MongoDB ativo em localhost:27017 (ex.: ./scripts/start-mongo.sh).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.data.mongodb.database=steamcatalog_test"
})
class SteamCatalogIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;
    @Autowired
    ImportService importService;
    @Autowired
    GameService gameService;
    @Autowired
    DeveloperRepository developerRepository;
    @Autowired
    PublisherRepository publisherRepository;
    @Autowired
    GameRepository gameRepository;

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @BeforeEach
    void clean() {
        gameRepository.deleteAll();
        developerRepository.deleteAll();
        publisherRepository.deleteAll();
    }

    @Test
    void importacaoViaThreadPopulaAsTresCollections() throws Exception {
        boolean started = importService.startImport("data/SteamGames_cleaned.csv", 1500);
        assertThat(started).isTrue();
        // segunda chamada nao deve iniciar outra importacao simultanea
        assertThat(importService.startImport("data/SteamGames_cleaned.csv", 10)).isFalse();

        long deadline = System.currentTimeMillis() + 60_000;
        while (importService.isRunning() && System.currentTimeMillis() < deadline) {
            Thread.sleep(200);
        }
        assertThat(importService.isRunning()).isFalse();
        assertThat(gameRepository.count()).isGreaterThan(0);
        assertThat(developerRepository.count()).isGreaterThan(0);
        assertThat(publisherRepository.count()).isGreaterThan(0);
    }

    @Test
    @SuppressWarnings("unchecked")
    void crudViaApiRespeitandoIntegridadeEValidacao() {
        // --- cadastra developer e publisher (como Professor/Aluno) ---
        ResponseEntity<Map> devResp = rest.postForEntity(url("/api/developers"),
                Map.of("name", "Estudio Teste", "country", "Brasil"), Map.class);
        assertThat(devResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String devId = (String) devResp.getBody().get("id");
        assertThat(devId).isNotBlank();

        ResponseEntity<Map> pubResp = rest.postForEntity(url("/api/publishers"),
                Map.of("name", "Editora Teste", "country", "Brasil"), Map.class);
        assertThat(pubResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String pubId = (String) pubResp.getBody().get("id");

        // --- nome duplicado deve dar 400 ---
        ResponseEntity<Map> dup = rest.postForEntity(url("/api/developers"),
                Map.of("name", "Estudio Teste"), Map.class);
        assertThat(dup.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // --- game com developer inexistente deve dar 409 (integridade) ---
        ResponseEntity<Map> badGame = rest.postForEntity(url("/api/games"),
                Map.of("appid", 5000001, "name", "X", "type", "game",
                        "developer", "NaoExiste", "publisher", "Editora Teste"), Map.class);
        assertThat(badGame.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // --- game com score invalido deve dar 400 (validacao) ---
        ResponseEntity<Map> badScore = rest.postForEntity(url("/api/games"),
                Map.of("appid", 5000002, "name", "Y", "type", "game", "reviewScore", 99,
                        "developer", "Estudio Teste", "publisher", "Editora Teste"), Map.class);
        assertThat(badScore.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // --- game valido deve ser criado (201) ---
        ResponseEntity<Map> okGame = rest.postForEntity(url("/api/games"),
                Map.of("appid", 5000003, "name", "Jogo Teste", "type", "game", "price", 49.9,
                        "reviewScore", 9, "rank", 1, "tags", List.of("action", "indie"),
                        "developer", "Estudio Teste", "publisher", "Editora Teste"), Map.class);
        assertThat(okGame.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(okGame.getBody().get("developer")).isEqualTo("Estudio Teste");

        // --- localizar por appid ---
        ResponseEntity<Map> byId = rest.getForEntity(url("/api/games/5000003"), Map.class);
        assertThat(byId.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(byId.getBody().get("name")).isEqualTo("Jogo Teste");

        // --- alterar (PUT) ---
        rest.put(url("/api/games/5000003"),
                Map.of("name", "Jogo Editado", "type", "game", "price", 9.9, "reviewScore", 7,
                        "developer", "Estudio Teste", "publisher", "Editora Teste"));
        Game reload = gameService.findByAppid(5000003);
        assertThat(reload.getName()).isEqualTo("Jogo Editado");
        assertThat(reload.getPrice()).isEqualTo(9.9);

        // --- integridade na exclusao: nao pode excluir dev/pub com jogo vinculado (409) ---
        ResponseEntity<Map> delDev = rest.exchange(url("/api/developers/" + devId),
                org.springframework.http.HttpMethod.DELETE, null, Map.class);
        assertThat(delDev.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // --- remove o jogo, ai a exclusao do developer passa ---
        rest.delete(url("/api/games/5000003"));
        ResponseEntity<Map> delDev2 = rest.exchange(url("/api/developers/" + devId),
                org.springframework.http.HttpMethod.DELETE, null, Map.class);
        assertThat(delDev2.getStatusCode()).isEqualTo(HttpStatus.OK);
        // publisher tambem fica livre
        ResponseEntity<Map> delPub = rest.exchange(url("/api/publishers/" + pubId),
                org.springframework.http.HttpMethod.DELETE, null, Map.class);
        assertThat(delPub.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void pesquisaComMultiplosCriterios() throws Exception {
        // importa um lote para ter dados reais
        importService.startImport("data/SteamGames_cleaned.csv", 1500);
        long deadline = System.currentTimeMillis() + 60_000;
        while (importService.isRunning() && System.currentTimeMillis() < deadline) {
            Thread.sleep(200);
        }

        // busca via servico: gratuitos (preco 0) com score >= 7
        List<Game> gratis = gameService.search(
                new GameSearchCriteria().maxPrice(0.0).minScore(7).limit(50));
        assertThat(gratis).allSatisfy(g -> {
            assertThat(g.getPrice()).isEqualTo(0.0);
            assertThat(g.getReviewScore()).isGreaterThanOrEqualTo(7);
        });

        // busca via API REST com nome + tipo + rank
        ResponseEntity<Map> resp = rest.getForEntity(
                url("/api/games?name=a&type=game&maxRank=1000&limit=10"), Map.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).containsKey("items");
        assertThat(resp.getBody()).containsKey("total");

        // FAIXA de preco (min E max no MESMO campo) - regressao do bug
        // "you can't add a second 'price' criteria"
        ResponseEntity<Map> faixa = rest.getForEntity(
                url("/api/games?minPrice=5&maxPrice=20&limit=10"), Map.class);
        assertThat(faixa.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Game> faixaServico = gameService.search(
                new GameSearchCriteria().minPrice(5.0).maxPrice(20.0).limit(50));
        assertThat(faixaServico).allSatisfy(g -> {
            assertThat(g.getPrice()).isGreaterThanOrEqualTo(5.0);
            assertThat(g.getPrice()).isLessThanOrEqualTo(20.0);
        });
    }
}
