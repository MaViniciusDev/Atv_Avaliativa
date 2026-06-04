# Steam Catalog — Atividade Avaliativa (2ª Unidade)

**Manipulação de Dados com Collections e Serviços** — **Spring Boot + MongoDB**

Aplicação **Spring Boot** que importa o dataset [Steam Games](https://www.kaggle.com/datasets)
(Kaggle) para **3 collections relacionadas** no MongoDB e oferece importação por
*threads*, CRUD, pesquisas com múltiplos critérios e **regras de integridade
referencial**, com uma interface web (tema inspirado na Steam).

---

## 1. Dataset escolhido

**Steam Games** (`data/SteamGames_cleaned.csv`, ~29 mil jogos): nome, tipo, data de
lançamento, desenvolvedora(s), distribuidora(s), preço, avaliações, tags, capa, etc.

## 2. Modelagem — 3 collections relacionadas

| Collection    | Papel no exemplo do enunciado | Descrição                                  |
|---------------|-------------------------------|--------------------------------------------|
| `developers`  | **Professor**                 | Desenvolvedora do jogo                     |
| `publishers`  | **Aluno**                     | Distribuidora/editora do jogo              |
| `games`       | **Turma**                     | Jogo — referencia 1 developer e 1 publisher|

Relacionamento **1:N** (um developer/publisher possui vários jogos). O `Game`
guarda `developerId` e `publisherId`, que apontam para o `id` dos documentos das
outras collections:

```
developers (id, name, country)        publishers (id, name, country)
      ▲                                       ▲
      │ developerId (FK)                       │ publisherId (FK)
      └──────────────  games  ────────────────┘
         (id, appid, name, type, price, reviewScore, rank, tags, thumbnail, ...)
```

Um `Game` só pode ser cadastrado se o developer e o publisher referenciados **já
existirem** — exatamente como a Turma exige Professor e Aluno previamente cadastrados.

---

## 3. Tecnologias

- **Spring Boot 3.4** (`spring-boot-starter-web` + `spring-boot-starter-data-mongodb`)
- **MongoDB** (Spring Data MongoDB: `MongoRepository` + `MongoTemplate`)
- **Java 21+** (testado com JDK 25)
- Front-end: HTML/CSS/JS puro servido como conteúdo estático pelo Spring

---

## 4. Como executar

### Pré-requisitos
- **JDK 17+** (testado com JDK 25)
- **Docker** (para o MongoDB) — ou um MongoDB já rodando em `localhost:27017`
- Não é necessário instalar o Maven: use o wrapper `./mvnw`

### Opção A — Docker Compose (sobe tudo com um comando)

```bash
docker compose up --build
#   sobe o MongoDB + a aplicação; depois acesse http://localhost:8090
#   (Ctrl+C encerra; "docker compose down" remove os containers)

# Se a 8090 ja estiver em uso (ex.: o app rodando pelo IDE), escolha outra
# porta de host sem editar nada:
APP_PORT=8095 docker compose up --build   # acesse http://localhost:8095
```

A porta do MongoDB **não** é exposta no host (para não conflitar com outro Mongo
local); a aplicação o acessa pela rede interna do compose. Os dados ficam no volume
`mongo-data` (sobrevivem a `docker compose down`).

> Não rode o app pelo IDE **e** o compose na mesma porta ao mesmo tempo — os dois
> disputariam a 8090. Use portas diferentes (via `APP_PORT`) ou apenas um deles.

### Opção B — rodar localmente (Maven)

```bash
# 1) Subir o MongoDB (Docker)
./scripts/start-mongo.sh
#   equivalente a: docker run -d --name mongo-steam -p 27017:27017 mongo:7

# 2) Subir a aplicação Spring Boot
./scripts/run.sh
#   ou diretamente:
./mvnw spring-boot:run

# 3) Abrir a interface no navegador
#   http://localhost:8090
#   Na primeira execução, vá em "Importar" e clique em "Iniciar importação"
#   para carregar o dataset (a carga roda em thread, ~5s para os ~29 mil jogos).

# 4) (opcional) Rodar os testes de integração que validam TODOS os requisitos
./scripts/test.sh        # ou ./mvnw test  (requer o MongoDB no ar)
```

> **Porta:** a aplicação usa a **8090** por padrão (a 8080 costuma estar ocupada).
> Altere com a variável `SERVER_PORT`.

### Gerar e rodar o JAR executável

```bash
./mvnw clean package
java -jar target/steam-catalog.jar
```

Configurações (via `application.properties` ou variáveis de ambiente):

| Variável        | Padrão                       | Descrição                |
|-----------------|------------------------------|--------------------------|
| `MONGODB_URI`   | `mongodb://localhost:27017`  | URI do MongoDB           |
| `MONGODB_DB`    | `steamcatalog`               | Nome do banco            |
| `SERVER_PORT`   | `8090`                       | Porta do servidor web    |

---

## 5. Interface e API

A interface web (em `src/main/resources/static`) é servida pelo próprio Spring e
consome a API REST. O visual é uma releitura **original** do tema escuro da Steam
(paleta navy + acentos em azul, grade de capas), sem usar logos/imagens proprietários.

- **Loja**: grade de jogos com capas; busca por múltiplos critérios; modal de
  detalhes; cadastrar/editar/excluir jogos.
- **Developers / Publishers**: tabela com busca + CRUD.
- **Importar**: dispara a importação (thread) e mostra o progresso em tempo real.

Endpoints (`@RestController`):

| Método | Rota | Função |
|---|---|---|
| GET | `/api/stats` | contagem das 3 collections |
| POST | `/api/import` | inicia a importação (thread) |
| GET | `/api/import/status` | progresso da importação |
| GET/POST/PUT/DELETE | `/api/developers[/{id}]` | CRUD + busca de developers |
| GET/POST/PUT/DELETE | `/api/publishers[/{id}]` | CRUD + busca de publishers |
| GET/POST/PUT/DELETE | `/api/games[/{appid}]` | CRUD + busca de jogos |

Um `@RestControllerAdvice` traduz as exceções de negócio: **400** (validação),
**409** (integridade) e **404** (não encontrado), sempre com corpo `{"error": "..."}`.

---

## 6. Mapa dos requisitos → código

| Requisito do enunciado | Onde está implementado |
|---|---|
| **3 collections relacionadas** | `model/Developer`, `model/Publisher`, `model/Game` (`@Document` + `@Indexed`) |
| **Importação via serviço/método** | `service/ImportService` |
| **Importação do CSV usando Threads, acionável a qualquer momento** | `ImportService.startImport()` cria a thread `csv-import-thread`; inserts num *pool* de 4 threads; estado em `service/ImportProgress`; acionável por `POST /api/import` |
| **Cadastrar item individualmente** | `*Service.create(...)` → `POST /api/...` |
| **Alterar registros** | `*Service.update(...)` → `PUT /api/...` |
| **Excluir registros** | `*Service.delete(...)` → `DELETE /api/...` |
| **Localizar item individualmente** | `DeveloperService.findByName`, `GameService.findByAppid` (via `MongoRepository`) |
| **Pesquisas com múltiplos parâmetros (AND)** | `GameService.search(GameSearchCriteria)` com `MongoTemplate`/`Criteria` — nome, tipo, faixa de preço, score, rank, tag, developer, publisher |
| **Integridade: Game exige Developer e Publisher existentes** | `GameService.create()` → `requireDeveloper()`/`requirePublisher()` lançam `IntegrityException` (409) |
| **Integridade: não excluir Developer/Publisher com vínculo** | `DeveloperService.delete()`/`PublisherService.delete()` checam `GameRepository.countBy...Id()` |
| **Validação de dados inválidos** | `GameService.validate()` (appid>0, preço≥0, score 0–10, data ISO, unicidade) e validação de nome nos demais services |
| **Índices únicos (integridade no banco)** | `@Indexed(unique=true)` em `developers.name`, `publishers.name`, `games.appid` (auto-criados: `spring.data.mongodb.auto-index-creation=true`) |

---

## 7. Estrutura do projeto

```
src/main/java/br/com/steamcatalog/
├── SteamCatalogApplication.java   # @SpringBootApplication (entrada)
├── model/                         # entidades @Document
│   ├── Developer.java  Publisher.java  Game.java
├── repository/                    # interfaces Spring Data (MongoRepository)
│   ├── DeveloperRepository.java  PublisherRepository.java  GameRepository.java
├── service/                       # regras de negocio (@Service)
│   ├── DeveloperService.java  PublisherService.java  GameService.java
│   ├── GameSearchCriteria.java
│   ├── ImportService.java         # importacao com threads
│   └── ImportProgress.java
├── controller/                    # @RestController
│   ├── StatsController.java  ImportController.java
│   ├── DeveloperController.java  PublisherController.java  GameController.java
├── dto/                           # records de request/response
│   ├── EntityRequest/Response  GameRequest/Response  ListResponse
├── exception/                     # excecoes + @RestControllerAdvice
│   ├── ValidationException  IntegrityException  NotFoundException  RestExceptionHandler
└── util/
    └── CsvReader.java             # parser CSV (RFC 4180) feito a mao

src/main/resources/
├── application.properties
└── static/                        # front-end (index.html, style.css, app.js)

src/test/java/br/com/steamcatalog/
└── SteamCatalogIntegrationTest.java  # @SpringBootTest valida os requisitos
```

---

## 8. Testes

`./scripts/test.sh` (ou `./mvnw test`) sobe o contexto Spring Boot real (servidor em
porta aleatória, banco `steamcatalog_test`) e valida, de ponta a ponta:

- importação via thread populando as 3 collections;
- CRUD individual pela API REST;
- regras de integridade (HTTP **409** ao cadastrar jogo sem developer/publisher e ao
  excluir developer/publisher vinculado);
- validação (HTTP **400** para dados inválidos);
- pesquisa com múltiplos critérios.
