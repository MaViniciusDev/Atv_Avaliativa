# Steam Catalog — Documentação para Apresentação

**Atividade Avaliativa – 2ª Unidade: Manipulação de Dados com Collections e Serviços**

> Aplicação **Spring Boot + MongoDB** que importa o dataset **Steam Games** (Kaggle)
> para **3 collections relacionadas**, com importação por *threads*, CRUD, pesquisas
> por múltiplos critérios, regras de integridade referencial e uma interface web.

---

## 1. Visão geral do projeto

O sistema é um **catálogo de jogos da Steam**. A partir de um arquivo CSV (~29 mil
jogos), os dados são distribuídos em três coleções que se relacionam entre si:

- **developers** — desenvolvedoras dos jogos
- **publishers** — distribuidoras/editoras dos jogos
- **games** — os jogos, que referenciam uma desenvolvedora e uma editora

O paralelo com o exemplo do enunciado (Professor / Aluno / Turma):

| Enunciado | Nosso projeto | Regra |
|-----------|---------------|-------|
| Professor | **Developer**  | cadastrado livremente |
| Aluno     | **Publisher**  | cadastrado livremente |
| Turma     | **Game**       | só existe se Developer **e** Publisher já existirem |

---

## 2. Tecnologias e arquitetura

**Stack:** Java 21 · Spring Boot 3.4 · Spring Web (API REST) · Spring Data MongoDB ·
MongoDB · front-end em HTML/CSS/JS puro · Docker / Docker Compose.

A aplicação segue uma **arquitetura em camadas**, cada uma com uma responsabilidade:

```
Navegador (front-end SPA)
        │  HTTP/JSON
        ▼
┌──────────────────────────────────────────────┐
│ controller/  →  @RestController (API REST)    │  recebe requisições, devolve JSON
├──────────────────────────────────────────────┤
│ service/     →  @Service (regras de negócio)  │  validação + integridade + threads
├──────────────────────────────────────────────┤
│ repository/  →  MongoRepository (Spring Data) │  acesso aos dados
├──────────────────────────────────────────────┤
│ model/       →  @Document (entidades)         │  mapeamento objeto ↔ documento
└──────────────────────────────────────────────┘
        │
        ▼
     MongoDB (developers, publishers, games)
```

Por que camadas? Separação de responsabilidades: o **controller** não conhece o
banco, o **service** concentra as regras (validação/integridade), e o **repository**
só persiste. Isso deixa o código testável e organizado.

### Estrutura de pastas

```
src/main/java/br/com/steamcatalog/
├── SteamCatalogApplication.java   # ponto de entrada (@SpringBootApplication)
├── model/                         # entidades @Document: Developer, Publisher, Game
├── repository/                    # interfaces Spring Data (MongoRepository)
├── service/                       # regras de negócio (@Service) + importação
├── controller/                    # endpoints REST (@RestController)
├── dto/                           # objetos de request/response (records)
├── exception/                     # exceções + tradutor para HTTP (@RestControllerAdvice)
└── util/CsvReader.java            # parser de CSV feito à mão

src/main/resources/
├── application.properties         # configuração (URI do Mongo, porta, índices)
└── static/                        # front-end: index.html, style.css, app.js

src/test/java/...                  # teste de integração que valida os requisitos
Dockerfile · docker-compose.yml    # empacotamento e execução com um comando
```

---

## 3. As 3 collections e o relacionamento (Requisito 1)

As entidades são anotadas com **Spring Data MongoDB**. Cada classe vira um documento
de uma collection.

**`model/Developer.java`** e **`model/Publisher.java`** (estrutura idêntica):

```java
@Document(collection = "developers")
public class Developer {
    @Id private String id;
    @Indexed(unique = true) private String name;   // nome único (integridade)
    private String country;
}
```

**`model/Game.java`** — guarda **referências** (chaves estrangeiras) para as outras
duas collections:

```java
@Document(collection = "games")
public class Game {
    @Id private String id;
    @Indexed(unique = true) private int appid;      // id do jogo na Steam (único)
    private String name, type, releaseDate, description, thumbnail;
    private double price;
    private int reviewScore, positiveReview, negativeReview, rank;
    @Indexed private String developerId;            // FK -> developers.id
    @Indexed private String publisherId;            // FK -> publishers.id
    private List<String> tags;
}
```

**Relacionamento 1:N** — uma desenvolvedora/editora tem vários jogos; cada jogo
aponta para **uma** desenvolvedora e **uma** editora pelo `id`:

```
developers (id, name, country)        publishers (id, name, country)
      ▲                                       ▲
      │ developerId                            │ publisherId
      └──────────────  games  ────────────────┘
```

Os **índices únicos** (`@Indexed(unique = true)`) em `developers.name`,
`publishers.name` e `games.appid` garantem, no próprio banco, que não há
duplicidade. São criados automaticamente (`spring.data.mongodb.auto-index-creation=true`).

---

## 4. Importação do CSV com Threads (Requisito 2)

**Arquivo:** `service/ImportService.java`

A importação roda **em segundo plano (thread)**, para que a aplicação continue
respondendo e o usuário possa dispará-la a qualquer momento (pelo botão "Importar"
da interface, que chama `POST /api/import`).

Como funciona:

1. `startImport()` cria uma **thread coordenadora** (`csv-import-thread`) e retorna
   imediatamente — a interface não trava.
2. A thread coordenadora lê o CSV **sequencialmente** (necessário porque a descrição
   de um jogo pode ter vírgulas, aspas e quebras de linha) e, para cada jogo:
   - resolve a desenvolvedora e a editora (cria se ainda não existirem) — assim
     elas existem **antes** do jogo (integridade);
   - envia a inserção do jogo para um **pool de 4 threads** (`ExecutorService`),
     acelerando a carga.
3. O progresso é exposto em `GET /api/import/status` (lidos, inseridos, ignorados,
   novos developers/publishers) e a interface mostra uma barra em tempo real.

```java
public boolean startImport(String csvPath, int limit) {
    if (!progress.start()) return false;          // evita duas importações ao mesmo tempo
    Thread worker = new Thread(() -> runImport(path, limit), "csv-import-thread");
    worker.setDaemon(true);
    worker.start();                               // roda em background
    return true;
}
```

**Pontos para falar:** uso de `Thread` + `ExecutorService` (pool), `AtomicBoolean`/
`AtomicInteger` para o progresso ser seguro entre threads, e o parser de CSV próprio
(`util/CsvReader.java`, padrão RFC 4180) que trata campos com vírgulas/aspas/quebras.

---

## 5. CRUD individual (Requisito 3)

Cada collection tem operações de **Cadastrar, Alterar, Excluir e Localizar**,
implementadas nos *services* e expostas como endpoints REST.

| Operação | Service | Endpoint |
|----------|---------|----------|
| Cadastrar | `create(...)` | `POST /api/{developers,publishers,games}` |
| Alterar | `update(...)` | `PUT /api/.../{id|appid}` |
| Excluir | `delete(...)` | `DELETE /api/.../{id|appid}` |
| Localizar | `findById` / `findByAppid` / `findByName` | `GET /api/games/{appid}` |

Os **repositories** são interfaces — o Spring Data implementa o CRUD básico e gera
as consultas a partir do nome do método:

```java
public interface GameRepository extends MongoRepository<Game, String> {
    Optional<Game> findByAppid(int appid);
    long countByDeveloperId(String developerId);   // usado na integridade
    long countByPublisherId(String publisherId);
}
```

---

## 6. Pesquisas com múltiplos critérios (Requisito 4)

**Arquivo:** `service/GameService.java` (método `search`) + `GameSearchCriteria`

A busca de jogos aceita **vários parâmetros opcionais combinados em E (AND)**:
nome, tipo, faixa de preço (mín/máx), score mínimo, rank máximo, tag, desenvolvedora
e editora. A consulta é montada **dinamicamente** com `MongoTemplate` + `Criteria`,
adicionando só os critérios preenchidos:

```java
Query query = new Query();
if (notBlank(c.getName()))  query.addCriteria(Criteria.where("name").regex(..., "i"));
if (notBlank(c.getType()))  query.addCriteria(Criteria.where("type").is(...));
// preço: min e max no MESMO campo combinados em um único critério
if (c.getMinPrice() != null || c.getMaxPrice() != null) {
    Criteria price = Criteria.where("price");
    if (c.getMinPrice() != null) price = price.gte(c.getMinPrice());
    if (c.getMaxPrice() != null) price = price.lte(c.getMaxPrice());
    query.addCriteria(price);
}
// developer/publisher informados por NOME e resolvidos para o id (relacionamento)
```

Endpoint: `GET /api/games?name=...&minPrice=...&maxPrice=...&minScore=...&developer=...`

**Detalhe interessante para a banca:** quando o usuário busca por desenvolvedora/
editora, o service **resolve o nome para o `id`** antes de filtrar — ou seja, a
pesquisa **navega pelo relacionamento** entre as collections.

---

## 7. Integridade referencial (Regras de Relacionamento)

Esta é a parte central do enunciado, implementada no **`GameService`**,
`DeveloperService` e `PublisherService`.

### 7.1. Cadastro de Game exige Developer e Publisher existentes

Ao cadastrar/alterar um jogo, o service verifica se a desenvolvedora e a editora
**já existem**. Se não existirem, lança `IntegrityException` (vira HTTP **409**):

```java
public Game create(Game game, String developerName, String publisherName) {
    validate(game);
    Developer dev = requireDeveloper(developerName);  // 409 se não existir
    Publisher pub = requirePublisher(publisherName);  // 409 se não existir
    game.setDeveloperId(dev.getId());
    game.setPublisherId(pub.getId());
    return repository.save(game);
}
```

### 7.2. Não excluir Developer/Publisher com jogos vinculados

Antes de excluir, conta quantos jogos referenciam aquele registro. Se houver
vínculo, **bloqueia** a exclusão:

```java
public void delete(String id) {
    Developer existing = repository.findById(id).orElseThrow(...);
    long vinculos = gameRepository.countByDeveloperId(id);
    if (vinculos > 0)
        throw new IntegrityException("Não é possível excluir... existem "
            + vinculos + " jogo(s) vinculado(s).");
    repository.deleteById(id);
}
```

---

## 8. Validação dos dados (Requisito Geral)

**Arquivo:** `service/GameService.java` (método `validate`)

Não são permitidas inserções inválidas. O service valida, por exemplo:

- `appid` inteiro **positivo** e **único**;
- nome e tipo **obrigatórios**;
- preço **≥ 0**;
- `reviewScore` entre **0 e 10**;
- data de lançamento no formato **ISO `yyyy-MM-dd`**;
- nome de developer/publisher **obrigatório e único**.

Quando algo é inválido, lança `ValidationException` (vira HTTP **400**).

### Tradução das exceções para HTTP

`exception/RestExceptionHandler.java` (`@RestControllerAdvice`) converte as exceções
de negócio em respostas padronizadas `{"error": "..."}`:

| Exceção | HTTP | Significado |
|---------|------|-------------|
| `ValidationException` | **400** | dados inválidos |
| `IntegrityException`  | **409** | violação de integridade |
| `NotFoundException`   | **404** | registro não encontrado |

---

## 9. A interface web

Front-end de página única (`src/main/resources/static`), servido pelo próprio Spring,
com visual de tema escuro inspirado na estética da Steam. Abas:

- **Loja** — grade de jogos com capas; busca por múltiplos critérios; cadastrar/
  editar/excluir jogos.
- **Developers / Publishers** — tabela com busca e CRUD.
- **Importar** — dispara a importação (thread) e mostra o progresso em tempo real.

### Navegação pelo relacionamento (drill-down)

Na lista de **Publishers** (e Developers), ao **clicar no nome** ou em **"Ver jogos"**,
abre-se um painel com **todos os jogos daquele registro** e um **resumo de dados
relacionados**: total de jogos, gratuitos/pagos, score médio e nº de desenvolvedoras
distintas. Cada jogo mostra a sua **desenvolvedora** (a outra collection), e clicando
nele vê-se o detalhe completo, com botão "← Voltar". Isso demonstra visualmente o
relacionamento entre as três collections.

> Exemplo real: a editora **Valve** publica "Garry's Mod", mas quem o **desenvolve**
> é a **Facepunch Studios** — o drill-down deixa esse relacionamento evidente.

---

## 10. Como executar (para a demonstração)

**Opção A — Docker Compose (um comando):**

```bash
docker compose up --build       # sobe MongoDB + aplicação
# abra http://localhost:8090   (ou APP_PORT=8095 docker compose up, se a 8090 estiver ocupada)
```

**Opção B — local (Maven):**

```bash
./scripts/start-mongo.sh        # MongoDB no Docker
./scripts/run.sh                # = ./mvnw spring-boot:run  → http://localhost:8090
```

> Na **primeira execução**, abra a aba **Importar** e clique em **"Iniciar
> importação"** para carregar o dataset (roda em thread, ~5s para ~29 mil jogos).

---

## 11. Testes automatizados

`src/test/java/.../SteamCatalogIntegrationTest.java` sobe o **contexto Spring Boot
real** (servidor + MongoDB) e valida, de ponta a ponta, **todos os requisitos**:

- importação via thread populando as 3 collections;
- CRUD individual pela API REST;
- integridade (HTTP **409** ao criar jogo sem developer/publisher e ao excluir
  registro vinculado);
- validação (HTTP **400**);
- pesquisa por múltiplos critérios (incluindo faixa de preço mín+máx).

Rodar: `./scripts/test.sh` (ou `./mvnw test`) — **3 testes, todos passando**.

---

## 12. Roteiro sugerido de apresentação (≈ 8–10 min)

1. **Contexto (1 min):** "Escolhi o dataset Steam Games do Kaggle e modelei em 3
   collections relacionadas — developers, publishers e games."
2. **Arquitetura (1 min):** mostrar o diagrama de camadas (controller → service →
   repository → MongoDB) e o porquê da separação.
3. **As 3 collections e o relacionamento (1 min):** abrir `Game.java` e mostrar
   `developerId`/`publisherId` + os índices únicos.
4. **Importação com threads (1–2 min):** abrir a aba Importar, clicar em importar e
   mostrar a barra de progresso enquanto navega em outra aba (prova que é assíncrono).
   Explicar `startImport()` e o pool de threads.
5. **CRUD + validação (1–2 min):** cadastrar um jogo na interface; tentar um inválido
   (score 99) e mostrar o erro **400**.
6. **Integridade (1–2 min):** tentar cadastrar um jogo com developer inexistente
   (**409**); tentar excluir um developer que tem jogos (**409**). Este é o ponto
   mais forte do enunciado.
7. **Pesquisa multi-critério + drill-down (1 min):** filtrar jogos por preço+score;
   depois ir em Publishers, clicar na Valve e mostrar os jogos dela com os dados
   relacionados.
8. **Fecho (30 s):** "Tudo coberto por um teste de integração automatizado e
   empacotado com Docker Compose."

---

## 13. Possíveis perguntas da banca (e respostas)

- **"Por que MongoDB e não SQL?"** O enunciado fala em *collections* e *documentos*;
  o MongoDB é orientado a documentos. O relacionamento é feito por referência de `id`
  (como chave estrangeira) e a integridade é garantida na **camada de serviço**.
- **"Como garante que não cadastra jogo sem developer?"** O `GameService.create`
  chama `requireDeveloper`/`requirePublisher`, que lançam `IntegrityException` (409)
  se a referência não existir.
- **"E a concorrência na importação?"** Uma thread coordenadora lê o arquivo e um
  pool de 4 threads insere os jogos; o progresso usa tipos atômicos (`Atomic*`), que
  são seguros entre threads; um `AtomicBoolean` impede duas importações simultâneas.
- **"Como evita nomes/appids duplicados?"** Índices únicos no MongoDB
  (`@Indexed(unique=true)`) + verificação na camada de serviço antes de salvar.
- **"Os relacionamentos respeitam integridade na exclusão?"** Sim: antes de excluir
  um developer/publisher, contamos os jogos vinculados (`countByDeveloperId`) e
  bloqueamos se houver algum.

---

## 14. Resumo: requisito → onde está no código

| Requisito do enunciado | Implementação |
|------------------------|---------------|
| 3 collections relacionadas | `model/Developer`, `model/Publisher`, `model/Game` (`@Document` + `@Indexed`) |
| Importação via serviço/método | `service/ImportService` |
| Importação do CSV com Threads, acionável a qualquer momento | `ImportService.startImport()` (thread `csv-import-thread` + pool de 4 threads); `POST /api/import` |
| Cadastrar individualmente | `*Service.create(...)` → `POST` |
| Alterar registros | `*Service.update(...)` → `PUT` |
| Excluir registros | `*Service.delete(...)` → `DELETE` |
| Localizar individualmente | `findByAppid` / `findByName` / `findById` |
| Pesquisas com múltiplos parâmetros | `GameService.search(GameSearchCriteria)` (`MongoTemplate` + `Criteria`) |
| Game exige Developer e Publisher existentes | `GameService.requireDeveloper/requirePublisher` → 409 |
| Não excluir Developer/Publisher vinculado | `*Service.delete` + `countBy...Id()` → 409 |
| Validação de dados inválidos | `GameService.validate()` → 400 |
| Integridade no banco (únicos) | `@Indexed(unique=true)` + `auto-index-creation` |
