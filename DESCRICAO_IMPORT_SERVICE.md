# Descrição linha por linha — `ImportService.java`

Arquivo analisado: `/tmp/workspace/MaViniciusDev/Atv_Avaliativa/src/main/java/br/com/steamcatalog/service/ImportService.java`

| Linha | Código | O que faz |
|---:|---|---|
| 1 | `package br.com.steamcatalog.service;` | Define o pacote Java onde a classe está localizada. |
| 2 | `` | Linha em branco para separar blocos lógicos. |
| 3 | `import java.io.FileReader;` | Importa uma classe necessária para o funcionamento do service. |
| 4 | `import java.io.IOException;` | Importa uma classe necessária para o funcionamento do service. |
| 5 | `import java.nio.charset.StandardCharsets;` | Importa uma classe necessária para o funcionamento do service. |
| 6 | `import java.nio.file.Files;` | Importa uma classe necessária para o funcionamento do service. |
| 7 | `import java.nio.file.Path;` | Importa uma classe necessária para o funcionamento do service. |
| 8 | `import java.util.ArrayList;` | Importa uma classe necessária para o funcionamento do service. |
| 9 | `import java.util.List;` | Importa uma classe necessária para o funcionamento do service. |
| 10 | `import java.util.concurrent.ConcurrentHashMap;` | Importa uma classe necessária para o funcionamento do service. |
| 11 | `import java.util.concurrent.ExecutorService;` | Importa uma classe necessária para o funcionamento do service. |
| 12 | `import java.util.concurrent.Executors;` | Importa uma classe necessária para o funcionamento do service. |
| 13 | `import java.util.concurrent.TimeUnit;` | Importa uma classe necessária para o funcionamento do service. |
| 14 | `` | Linha em branco para separar blocos lógicos. |
| 15 | `import org.springframework.dao.DuplicateKeyException;` | Importa uma classe necessária para o funcionamento do service. |
| 16 | `import org.springframework.stereotype.Service;` | Importa uma classe necessária para o funcionamento do service. |
| 17 | `` | Linha em branco para separar blocos lógicos. |
| 18 | `import br.com.steamcatalog.model.Developer;` | Importa uma classe necessária para o funcionamento do service. |
| 19 | `import br.com.steamcatalog.model.Game;` | Importa uma classe necessária para o funcionamento do service. |
| 20 | `import br.com.steamcatalog.model.Publisher;` | Importa uma classe necessária para o funcionamento do service. |
| 21 | `import br.com.steamcatalog.repository.DeveloperRepository;` | Importa uma classe necessária para o funcionamento do service. |
| 22 | `import br.com.steamcatalog.repository.GameRepository;` | Importa uma classe necessária para o funcionamento do service. |
| 23 | `import br.com.steamcatalog.repository.PublisherRepository;` | Importa uma classe necessária para o funcionamento do service. |
| 24 | `import br.com.steamcatalog.util.CsvReader;` | Importa uma classe necessária para o funcionamento do service. |
| 25 | `` | Linha em branco para separar blocos lógicos. |
| 26 | `/**` | Comentário Javadoc explicando a regra/objetivo do trecho. |
| 27 | ` * Servico de importacao do CSV do dataset Steam Games.` | Comentário Javadoc explicando a regra/objetivo do trecho. |
| 28 | ` *` | Comentário Javadoc explicando a regra/objetivo do trecho. |
| 29 | ` * REQUISITO: a importacao roda em uma THREAD em segundo plano, de modo que a` | Comentário Javadoc explicando a regra/objetivo do trecho. |
| 30 | ` * aplicacao permaneca responsiva e o usuario possa dispara-la a qualquer momento.` | Comentário Javadoc explicando a regra/objetivo do trecho. |
| 31 | ` *` | Comentário Javadoc explicando a regra/objetivo do trecho. |
| 32 | ` * Estrategia:` | Comentário Javadoc explicando a regra/objetivo do trecho. |
| 33 | ` *  - Uma thread "coordenadora" le o CSV sequencialmente (necessario porque um` | Comentário Javadoc explicando a regra/objetivo do trecho. |
| 34 | ` *    registro pode ocupar varias linhas) e resolve Developer/Publisher,` | Comentário Javadoc explicando a regra/objetivo do trecho. |
| 35 | ` *    garantindo que existam ANTES do jogo (integridade);` | Comentário Javadoc explicando a regra/objetivo do trecho. |
| 36 | ` *  - Os INSERTs dos jogos sao distribuidos para um pool de threads.` | Comentário Javadoc explicando a regra/objetivo do trecho. |
| 37 | ` */` | Comentário Javadoc explicando a regra/objetivo do trecho. |
| 38 | `@Service` | Marca a classe como componente de serviço gerenciado pelo Spring. |
| 39 | `public class ImportService {` | Declara a classe principal do serviço de importação. |
| 40 | `` | Linha em branco para separar blocos lógicos. |
| 41 | `    // Indices das colunas no arquivo SteamGames_cleaned.csv` | Comentário de apoio para explicar decisão de implementação. |
| 42 | `    private static final int COL_APPID = 0;` | Declara o índice de uma coluna do CSV para leitura por posição. |
| 43 | `    private static final int COL_NAME = 1;` | Declara o índice de uma coluna do CSV para leitura por posição. |
| 44 | `    private static final int COL_TYPE = 2;` | Declara o índice de uma coluna do CSV para leitura por posição. |
| 45 | `    private static final int COL_RELEASE = 3;` | Declara o índice de uma coluna do CSV para leitura por posição. |
| 46 | `    private static final int COL_DEVELOPERS = 4;` | Declara o índice de uma coluna do CSV para leitura por posição. |
| 47 | `    private static final int COL_PUBLISHERS = 5;` | Declara o índice de uma coluna do CSV para leitura por posição. |
| 48 | `    private static final int COL_DESCRIPTION = 6;` | Declara o índice de uma coluna do CSV para leitura por posição. |
| 49 | `    private static final int COL_PRICE = 7;` | Declara o índice de uma coluna do CSV para leitura por posição. |
| 50 | `    private static final int COL_THUMBNAIL = 8;` | Declara o índice de uma coluna do CSV para leitura por posição. |
| 51 | `    private static final int COL_REVIEWSCORE = 9;` | Declara o índice de uma coluna do CSV para leitura por posição. |
| 52 | `    private static final int COL_POSITIVE = 10;` | Declara o índice de uma coluna do CSV para leitura por posição. |
| 53 | `    private static final int COL_NEGATIVE = 11;` | Declara o índice de uma coluna do CSV para leitura por posição. |
| 54 | `    private static final int COL_RANK = 14;` | Declara o índice de uma coluna do CSV para leitura por posição. |
| 55 | `    private static final int COL_TAGS = 18;` | Declara o índice de uma coluna do CSV para leitura por posição. |
| 56 | `` | Linha em branco para separar blocos lógicos. |
| 57 | `    private static final int WORKER_THREADS = 4;` | Define quantas threads serão usadas para inserir jogos em paralelo. |
| 58 | `    private static final String DEFAULT_CSV = "data/SteamGames_cleaned.csv";` | Define o caminho padrão do arquivo CSV quando nenhum caminho é informado. |
| 59 | `` | Linha em branco para separar blocos lógicos. |
| 60 | `    private final DeveloperRepository developerRepository;` | Declara uma dependência de repositório usada para acessar o MongoDB. |
| 61 | `    private final PublisherRepository publisherRepository;` | Declara uma dependência de repositório usada para acessar o MongoDB. |
| 62 | `    private final GameRepository gameRepository;` | Declara uma dependência de repositório usada para acessar o MongoDB. |
| 63 | `` | Linha em branco para separar blocos lógicos. |
| 64 | `    // caches nome -> id para evitar consultas repetidas durante a carga` | Comentário de apoio para explicar decisão de implementação. |
| 65 | `    private final ConcurrentHashMap<String, String> developerCache = new ConcurrentHashMap<>();` | Declara cache em memória para reduzir buscas repetidas no banco. |
| 66 | `    private final ConcurrentHashMap<String, String> publisherCache = new ConcurrentHashMap<>();` | Declara cache em memória para reduzir buscas repetidas no banco. |
| 67 | `` | Linha em branco para separar blocos lógicos. |
| 68 | `    private final ImportProgress progress = new ImportProgress();` | Mantém o estado/progresso da importação em execução. |
| 69 | `` | Linha em branco para separar blocos lógicos. |
| 70 | `    public ImportService(DeveloperRepository developerRepository,` | Construtor que recebe as dependências por injeção do Spring. |
| 71 | `                         PublisherRepository publisherRepository,` | Executa parte do fluxo descrito pelo bloco onde esta linha está inserida. |
| 72 | `                         GameRepository gameRepository) {` | Abre um novo bloco de código (método, condição ou laço). |
| 73 | `        this.developerRepository = developerRepository;` | Atribui a dependência recebida para uso interno da classe. |
| 74 | `        this.publisherRepository = publisherRepository;` | Atribui a dependência recebida para uso interno da classe. |
| 75 | `        this.gameRepository = gameRepository;` | Atribui a dependência recebida para uso interno da classe. |
| 76 | `    }` | Delimita o bloco de código da estrutura atual. |
| 77 | `` | Linha em branco para separar blocos lógicos. |
| 78 | `    public ImportProgress getProgress() {` | Expõe o objeto de progresso para consulta externa. |
| 79 | `        return progress;` | Executa parte do fluxo descrito pelo bloco onde esta linha está inserida. |
| 80 | `    }` | Delimita o bloco de código da estrutura atual. |
| 81 | `` | Linha em branco para separar blocos lógicos. |
| 82 | `    public boolean isRunning() {` | Informa se já existe uma importação ativa no momento. |
| 83 | `        return progress.isRunning();` | Executa parte do fluxo descrito pelo bloco onde esta linha está inserida. |
| 84 | `    }` | Delimita o bloco de código da estrutura atual. |
| 85 | `` | Linha em branco para separar blocos lógicos. |
| 86 | `    /**` | Comentário Javadoc explicando a regra/objetivo do trecho. |
| 87 | `     * Dispara a importacao em uma thread separada e retorna imediatamente.` | Comentário Javadoc explicando a regra/objetivo do trecho. |
| 88 | `     *` | Comentário Javadoc explicando a regra/objetivo do trecho. |
| 89 | `     * @param csvPath caminho do arquivo CSV (vazio = caminho padrao)` | Comentário Javadoc explicando a regra/objetivo do trecho. |
| 90 | `     * @param limit   numero maximo de jogos a importar (0 = todos)` | Comentário Javadoc explicando a regra/objetivo do trecho. |
| 91 | `     * @return true se a importacao foi iniciada; false se ja havia uma rodando.` | Comentário Javadoc explicando a regra/objetivo do trecho. |
| 92 | `     */` | Comentário Javadoc explicando a regra/objetivo do trecho. |
| 93 | `    public boolean startImport(String csvPath, int limit) {` | Método público para iniciar a importação assíncrona. |
| 94 | `        if (!progress.start()) {` | Tenta marcar a importação como iniciada; evita concorrência de duas execuções. |
| 95 | `            return false; // ja existe uma importacao em andamento` | Retorna falso para indicar que não iniciou porque já havia uma execução ativa. |
| 96 | `        }` | Delimita o bloco de código da estrutura atual. |
| 97 | `        String path = (csvPath == null \|\| csvPath.isBlank()) ? DEFAULT_CSV : csvPath;` | Escolhe o caminho padrão quando o usuário não informa um arquivo válido. |
| 98 | `        Thread worker = new Thread(() -> runImport(path, limit), "csv-import-thread");` | Cria a thread coordenadora que executará a leitura e processamento do CSV. |
| 99 | `        worker.setDaemon(true);` | Define a thread como daemon para não bloquear o encerramento da aplicação. |
| 100 | `        worker.start();` | Dispara a execução assíncrona da importação. |
| 101 | `        return true;` | Retorna sucesso indicando que a importação foi iniciada. |
| 102 | `    }` | Delimita o bloco de código da estrutura atual. |
| 103 | `` | Linha em branco para separar blocos lógicos. |
| 104 | `    private void runImport(String csvPath, int limit) {` | Método interno que contém o fluxo completo da importação. |
| 105 | `        Path path = Path.of(csvPath);` | Converte a string recebida para objeto de caminho do sistema. |
| 106 | `        if (!Files.exists(path)) {` | Valida se o arquivo informado realmente existe antes de tentar ler. |
| 107 | `            progress.finish("ERRO: arquivo nao encontrado: " + csvPath);` | Finaliza o processo com status de erro quando o arquivo não é encontrado. |
| 108 | `            return;` | Executa parte do fluxo descrito pelo bloco onde esta linha está inserida. |
| 109 | `        }` | Delimita o bloco de código da estrutura atual. |
| 110 | `        preloadCaches();` | Carrega caches de developer/publisher com dados atuais do banco. |
| 111 | `` | Linha em branco para separar blocos lógicos. |
| 112 | `        ExecutorService pool = Executors.newFixedThreadPool(WORKER_THREADS);` | Cria um pool fixo de workers para inserção de jogos em paralelo. |
| 113 | `        try (CsvReader csv = new CsvReader(new FileReader(path.toFile(), StandardCharsets.UTF_8))) {` | Abre o leitor CSV em bloco try-with-resources para fechamento automático. |
| 114 | `            csv.readRecord(); // descarta o cabecalho` | Lê e ignora o cabeçalho do CSV antes de processar os dados. |
| 115 | `` | Linha em branco para separar blocos lógicos. |
| 116 | `            List<String> record;` | Executa parte do fluxo descrito pelo bloco onde esta linha está inserida. |
| 117 | `            int imported = 0;` | Executa parte do fluxo descrito pelo bloco onde esta linha está inserida. |
| 118 | `            while ((record = csv.readRecord()) != null) {` | Itera por cada registro do CSV até o fim do arquivo. |
| 119 | `                if (record.isEmpty() \|\| record.size() < COL_TAGS + 1) {` | Ignora linhas inválidas/incompletas que não possuem todas as colunas necessárias. |
| 120 | `                    continue;` | Executa parte do fluxo descrito pelo bloco onde esta linha está inserida. |
| 121 | `                }` | Delimita o bloco de código da estrutura atual. |
| 122 | `                progress.incRead();` | Incrementa o contador de registros lidos. |
| 123 | `                try {` | Abre um novo bloco de código (método, condição ou laço). |
| 124 | `                    Game game = buildGame(record);` | Transforma a linha do CSV em um objeto Game já normalizado. |
| 125 | `                    if (game == null) {` | Descarta o registro quando ele não atende os mínimos para criar jogo. |
| 126 | `                        progress.incSkipped();` | Incrementa o contador de registros descartados/falhos. |
| 127 | `                        continue;` | Executa parte do fluxo descrito pelo bloco onde esta linha está inserida. |
| 128 | `                    }` | Delimita o bloco de código da estrutura atual. |
| 129 | `                    game.setDeveloperId(resolveDeveloper(firstToken(record.get(COL_DEVELOPERS))));` | Resolve e define o ID do developer (criando-o se ainda não existir). |
| 130 | `                    game.setPublisherId(resolvePublisher(firstToken(record.get(COL_PUBLISHERS))));` | Resolve e define o ID do publisher (criando-o se ainda não existir). |
| 131 | `` | Linha em branco para separar blocos lógicos. |
| 132 | `                    pool.submit(() -> insertGame(game));` | Envia a inserção do jogo para execução concorrente no pool de threads. |
| 133 | `` | Linha em branco para separar blocos lógicos. |
| 134 | `                    imported++;` | Incrementa o total de jogos preparados para importação. |
| 135 | `                    if (limit > 0 && imported >= limit) {` | Interrompe a leitura quando o limite solicitado é atingido. |
| 136 | `                        break;` | Executa parte do fluxo descrito pelo bloco onde esta linha está inserida. |
| 137 | `                    }` | Delimita o bloco de código da estrutura atual. |
| 138 | `                } catch (Exception e) {` | Captura qualquer erro não previsto para evitar travamento silencioso. |
| 139 | `                    progress.incSkipped();` | Incrementa o contador de registros descartados/falhos. |
| 140 | `                }` | Delimita o bloco de código da estrutura atual. |
| 141 | `            }` | Delimita o bloco de código da estrutura atual. |
| 142 | `` | Linha em branco para separar blocos lógicos. |
| 143 | `            pool.shutdown();` | Inicia o encerramento ordenado do pool após terminar os envios. |
| 144 | `            pool.awaitTermination(10, TimeUnit.MINUTES);` | Aguarda até 10 minutos para as tasks pendentes concluírem. |
| 145 | `            progress.finish("Importacao concluida com sucesso.");` | Marca a importação como finalizada com mensagem de sucesso. |
| 146 | `        } catch (IOException e) {` | Captura falhas de leitura do arquivo CSV. |
| 147 | `            pool.shutdownNow();` | Força o encerramento imediato do pool em caso de erro grave. |
| 148 | `            progress.finish("ERRO de leitura do CSV: " + e.getMessage());` | Registra no progresso que houve falha de leitura do arquivo. |
| 149 | `        } catch (InterruptedException e) {` | Trata interrupções de thread durante a espera do pool. |
| 150 | `            Thread.currentThread().interrupt();` | Restaura o estado de interrupção da thread atual. |
| 151 | `            progress.finish("Importacao interrompida.");` | Finaliza com status específico de importação interrompida. |
| 152 | `        } catch (Exception e) {` | Captura qualquer erro não previsto para evitar travamento silencioso. |
| 153 | `            pool.shutdownNow();` | Força o encerramento imediato do pool em caso de erro grave. |
| 154 | `            progress.finish("ERRO inesperado: " + e.getMessage());` | Finaliza com mensagem genérica de erro inesperado. |
| 155 | `        }` | Delimita o bloco de código da estrutura atual. |
| 156 | `    }` | Delimita o bloco de código da estrutura atual. |
| 157 | `` | Linha em branco para separar blocos lógicos. |
| 158 | `    /**` | Comentário Javadoc explicando a regra/objetivo do trecho. |
| 159 | `     * Recarrega os caches a partir do estado ATUAL do banco. Limpa antes para` | Comentário Javadoc explicando a regra/objetivo do trecho. |
| 160 | `     * nao reaproveitar ids de uma carga anterior (importante se a base tiver` | Comentário Javadoc explicando a regra/objetivo do trecho. |
| 161 | `     * sido esvaziada entre importacoes).` | Comentário Javadoc explicando a regra/objetivo do trecho. |
| 162 | `     */` | Comentário Javadoc explicando a regra/objetivo do trecho. |
| 163 | `    private void preloadCaches() {` | Método que recarrega os caches de nomes para IDs a partir do banco atual. |
| 164 | `        developerCache.clear();` | Limpa o cache anterior para evitar IDs desatualizados. |
| 165 | `        publisherCache.clear();` | Limpa o cache anterior para evitar IDs desatualizados. |
| 166 | `        for (Developer d : developerRepository.findAll()) {` | Percorre todos os developers existentes no banco. |
| 167 | `            if (d.getName() != null) {` | Abre um novo bloco de código (método, condição ou laço). |
| 168 | `                developerCache.put(d.getName(), d.getId());` | Armazena no cache o par nome->id para acesso rápido. |
| 169 | `            }` | Delimita o bloco de código da estrutura atual. |
| 170 | `        }` | Delimita o bloco de código da estrutura atual. |
| 171 | `        for (Publisher p : publisherRepository.findAll()) {` | Percorre todos os publishers existentes no banco. |
| 172 | `            if (p.getName() != null) {` | Abre um novo bloco de código (método, condição ou laço). |
| 173 | `                publisherCache.put(p.getName(), p.getId());` | Armazena no cache o par nome->id para acesso rápido. |
| 174 | `            }` | Delimita o bloco de código da estrutura atual. |
| 175 | `        }` | Delimita o bloco de código da estrutura atual. |
| 176 | `    }` | Delimita o bloco de código da estrutura atual. |
| 177 | `` | Linha em branco para separar blocos lógicos. |
| 178 | `    private Game buildGame(List<String> row) {` | Método que mapeia uma linha CSV para objeto Game com sanitização. |
| 179 | `        int appid = parseInt(row.get(COL_APPID), -1);` | Converte o appid para inteiro usando valor padrão quando inválido. |
| 180 | `        String name = safe(row.get(COL_NAME));` | Lê e limpa o nome do jogo removendo espaços extras. |
| 181 | `        if (appid <= 0 \|\| name.isBlank()) {` | Valida os campos mínimos (appid positivo e nome preenchido). |
| 182 | `            return null;` | Retorna nulo para indicar que o registro deve ser descartado. |
| 183 | `        }` | Delimita o bloco de código da estrutura atual. |
| 184 | `        Game game = new Game();` | Instancia o objeto Game que será preenchido com dados do CSV. |
| 185 | `        game.setAppid(appid);` | Define o identificador único do jogo (appid). |
| 186 | `        game.setName(name);` | Define o nome do jogo no objeto. |
| 187 | `        String type = safe(row.get(COL_TYPE));` | Lê e normaliza o tipo do jogo a partir da coluna correspondente. |
| 188 | `        game.setType(type.isBlank() ? "game" : type.toLowerCase());` | Define tipo padrão "game" quando vazio; caso contrário usa minúsculas. |
| 189 | `        game.setReleaseDate(normalizeDate(row.get(COL_RELEASE)));` | Define data apenas se estiver no formato esperado yyyy-MM-dd. |
| 190 | `        game.setPrice(parseDouble(row.get(COL_PRICE), 0.0));` | Converte o preço para double com fallback para 0.0. |
| 191 | `        game.setDescription(truncate(safe(row.get(COL_DESCRIPTION)), 500));` | Define descrição já truncada para limitar tamanho máximo. |
| 192 | `        String thumb = safe(row.get(COL_THUMBNAIL));` | Lê e limpa a URL da thumbnail. |
| 193 | `        game.setThumbnail(thumb.startsWith("http") ? thumb : null);` | Só mantém thumbnail quando a URL parece válida (inicia com http). |
| 194 | `        game.setReviewScore(clamp(parseInt(row.get(COL_REVIEWSCORE), 0), 0, 10));` | Converte e limita o reviewScore ao intervalo permitido (0 a 10). |
| 195 | `        game.setPositiveReview(Math.max(0, parseInt(row.get(COL_POSITIVE), 0)));` | Garante contagem de reviews positivos não negativa. |
| 196 | `        game.setNegativeReview(Math.max(0, parseInt(row.get(COL_NEGATIVE), 0)));` | Garante contagem de reviews negativos não negativa. |
| 197 | `        game.setRank(Math.max(0, parseInt(row.get(COL_RANK), 0)));` | Garante rank não negativo ao preencher o objeto. |
| 198 | `        game.setTags(parseTags(row.get(COL_TAGS)));` | Converte a string de tags em lista normalizada. |
| 199 | `        return game;` | Executa parte do fluxo descrito pelo bloco onde esta linha está inserida. |
| 200 | `    }` | Delimita o bloco de código da estrutura atual. |
| 201 | `` | Linha em branco para separar blocos lógicos. |
| 202 | `    private void insertGame(Game game) {` | Método responsável por persistir o Game no banco. |
| 203 | `        try {` | Abre um novo bloco de código (método, condição ou laço). |
| 204 | `            gameRepository.save(game);` | Tenta salvar o jogo no MongoDB. |
| 205 | `            progress.incInserted();` | Incrementa o contador de jogos efetivamente inseridos. |
| 206 | `        } catch (DuplicateKeyException e) {` | Trata colisão de chave única (appid repetido). |
| 207 | `            progress.incSkipped(); // appid duplicado (indice unico)` | Incrementa o contador de registros descartados/falhos. |
| 208 | `        } catch (Exception e) {` | Captura qualquer erro não previsto para evitar travamento silencioso. |
| 209 | `            progress.incSkipped();` | Incrementa o contador de registros descartados/falhos. |
| 210 | `        }` | Delimita o bloco de código da estrutura atual. |
| 211 | `    }` | Delimita o bloco de código da estrutura atual. |
| 212 | `` | Linha em branco para separar blocos lógicos. |
| 213 | `    private String resolveDeveloper(String name) {` | Resolve (ou cria) um developer e retorna seu ID. |
| 214 | `        String key = (name == null \|\| name.isBlank()) ? "Desconhecido" : name.trim();` | Normaliza nome vazio para "Desconhecido" e remove espaços extras. |
| 215 | `        return developerCache.computeIfAbsent(key, k -> {` | Usa cache atômico para evitar buscas/criações duplicadas em concorrência. |
| 216 | `            try {` | Abre um novo bloco de código (método, condição ou laço). |
| 217 | `                Developer dev = developerRepository.save(new Developer(k));` | Cria developer novo quando ele ainda não existe. |
| 218 | `                progress.incDevelopersCreated();` | Incrementa contador de developers criados durante importação. |
| 219 | `                return dev.getId();` | Executa parte do fluxo descrito pelo bloco onde esta linha está inserida. |
| 220 | `            } catch (DuplicateKeyException dup) {` | Se outra thread criou antes, busca o ID existente no banco. |
| 221 | `                return developerRepository.findByName(k).map(Developer::getId).orElse(null);` | Recupera o ID da entidade já existente a partir do nome. |
| 222 | `            }` | Delimita o bloco de código da estrutura atual. |
| 223 | `        });` | Delimita o bloco de código da estrutura atual. |
| 224 | `    }` | Delimita o bloco de código da estrutura atual. |
| 225 | `` | Linha em branco para separar blocos lógicos. |
| 226 | `    private String resolvePublisher(String name) {` | Resolve (ou cria) um publisher e retorna seu ID. |
| 227 | `        String key = (name == null \|\| name.isBlank()) ? "Desconhecido" : name.trim();` | Normaliza nome vazio para "Desconhecido" e remove espaços extras. |
| 228 | `        return publisherCache.computeIfAbsent(key, k -> {` | Usa cache atômico para evitar buscas/criações duplicadas em concorrência. |
| 229 | `            try {` | Abre um novo bloco de código (método, condição ou laço). |
| 230 | `                Publisher pub = publisherRepository.save(new Publisher(k));` | Cria publisher novo quando ele ainda não existe. |
| 231 | `                progress.incPublishersCreated();` | Incrementa contador de publishers criados durante importação. |
| 232 | `                return pub.getId();` | Executa parte do fluxo descrito pelo bloco onde esta linha está inserida. |
| 233 | `            } catch (DuplicateKeyException dup) {` | Se outra thread criou antes, busca o ID existente no banco. |
| 234 | `                return publisherRepository.findByName(k).map(Publisher::getId).orElse(null);` | Recupera o ID da entidade já existente a partir do nome. |
| 235 | `            }` | Delimita o bloco de código da estrutura atual. |
| 236 | `        });` | Delimita o bloco de código da estrutura atual. |
| 237 | `    }` | Delimita o bloco de código da estrutura atual. |
| 238 | `` | Linha em branco para separar blocos lógicos. |
| 239 | `    // ----- helpers de parsing -----` | Comentário de apoio para explicar decisão de implementação. |
| 240 | `` | Linha em branco para separar blocos lógicos. |
| 241 | `    private static String firstToken(String value) {` | Extrai apenas o primeiro item de um texto separado por vírgula. |
| 242 | `        if (value == null) {` | Abre um novo bloco de código (método, condição ou laço). |
| 243 | `            return null;` | Retorna nulo para indicar que o registro deve ser descartado. |
| 244 | `        }` | Delimita o bloco de código da estrutura atual. |
| 245 | `        String v = value.trim();` | Executa parte do fluxo descrito pelo bloco onde esta linha está inserida. |
| 246 | `        int comma = v.indexOf(',');` | Localiza a primeira vírgula para identificar separação de múltiplos nomes. |
| 247 | `        return (comma >= 0) ? v.substring(0, comma).trim() : v;` | Retorna o primeiro token quando houver vírgula; senão retorna valor inteiro. |
| 248 | `    }` | Delimita o bloco de código da estrutura atual. |
| 249 | `` | Linha em branco para separar blocos lógicos. |
| 250 | `    private static List<String> parseTags(String value) {` | Converte a coluna de tags em lista de strings limpas e minúsculas. |
| 251 | `        List<String> tags = new ArrayList<>();` | Cria lista que receberá as tags processadas. |
| 252 | `        if (value == null \|\| value.isBlank()) {` | Aplica valor padrão quando o campo está ausente ou vazio. |
| 253 | `            return tags;` | Executa parte do fluxo descrito pelo bloco onde esta linha está inserida. |
| 254 | `        }` | Delimita o bloco de código da estrutura atual. |
| 255 | `        for (String t : value.split(",")) {` | Percorre cada tag separada por vírgula. |
| 256 | `            String tag = t.trim().toLowerCase();` | Normaliza a tag removendo espaços e convertendo para minúsculas. |
| 257 | `            if (!tag.isEmpty()) {` | Ignora tags vazias após o processamento. |
| 258 | `                tags.add(tag);` | Adiciona tag válida à lista final. |
| 259 | `            }` | Delimita o bloco de código da estrutura atual. |
| 260 | `        }` | Delimita o bloco de código da estrutura atual. |
| 261 | `        return tags;` | Executa parte do fluxo descrito pelo bloco onde esta linha está inserida. |
| 262 | `    }` | Delimita o bloco de código da estrutura atual. |
| 263 | `` | Linha em branco para separar blocos lógicos. |
| 264 | `    private static String normalizeDate(String value) {` | Valida data com regex simples e retorna null quando formato é inválido. |
| 265 | `        if (value == null) {` | Abre um novo bloco de código (método, condição ou laço). |
| 266 | `            return null;` | Retorna nulo para indicar que o registro deve ser descartado. |
| 267 | `        }` | Delimita o bloco de código da estrutura atual. |
| 268 | `        String v = value.trim();` | Executa parte do fluxo descrito pelo bloco onde esta linha está inserida. |
| 269 | `        return v.matches("\\d{4}-\\d{2}-\\d{2}") ? v : null;` | Mantém a data apenas se seguir o padrão yyyy-MM-dd. |
| 270 | `    }` | Delimita o bloco de código da estrutura atual. |
| 271 | `` | Linha em branco para separar blocos lógicos. |
| 272 | `    private static String safe(String value) {` | Evita null retornando string vazia e faz trim no conteúdo. |
| 273 | `        return value == null ? "" : value.trim();` | Executa parte do fluxo descrito pelo bloco onde esta linha está inserida. |
| 274 | `    }` | Delimita o bloco de código da estrutura atual. |
| 275 | `` | Linha em branco para separar blocos lógicos. |
| 276 | `    private static String truncate(String value, int max) {` | Limita tamanho de texto para não exceder o máximo definido. |
| 277 | `        if (value == null) {` | Abre um novo bloco de código (método, condição ou laço). |
| 278 | `            return null;` | Retorna nulo para indicar que o registro deve ser descartado. |
| 279 | `        }` | Delimita o bloco de código da estrutura atual. |
| 280 | `        return value.length() <= max ? value : value.substring(0, max);` | Retorna o valor original ou recorta até o tamanho máximo. |
| 281 | `    }` | Delimita o bloco de código da estrutura atual. |
| 282 | `` | Linha em branco para separar blocos lógicos. |
| 283 | `    private static int parseInt(String value, int def) {` | Converte texto para inteiro com fallback quando inválido. |
| 284 | `        if (value == null \|\| value.isBlank()) {` | Aplica valor padrão quando o campo está ausente ou vazio. |
| 285 | `            return def;` | Executa parte do fluxo descrito pelo bloco onde esta linha está inserida. |
| 286 | `        }` | Delimita o bloco de código da estrutura atual. |
| 287 | `        try {` | Abre um novo bloco de código (método, condição ou laço). |
| 288 | `            return (int) Double.parseDouble(value.trim());` | Permite interpretar números com decimal convertendo para inteiro. |
| 289 | `        } catch (NumberFormatException e) {` | Captura erro de parsing numérico e usa valor padrão. |
| 290 | `            return def;` | Executa parte do fluxo descrito pelo bloco onde esta linha está inserida. |
| 291 | `        }` | Delimita o bloco de código da estrutura atual. |
| 292 | `    }` | Delimita o bloco de código da estrutura atual. |
| 293 | `` | Linha em branco para separar blocos lógicos. |
| 294 | `    private static double parseDouble(String value, double def) {` | Converte texto para double com fallback quando inválido. |
| 295 | `        if (value == null \|\| value.isBlank()) {` | Aplica valor padrão quando o campo está ausente ou vazio. |
| 296 | `            return def;` | Executa parte do fluxo descrito pelo bloco onde esta linha está inserida. |
| 297 | `        }` | Delimita o bloco de código da estrutura atual. |
| 298 | `        try {` | Abre um novo bloco de código (método, condição ou laço). |
| 299 | `            return Double.parseDouble(value.trim().replace("$", ""));` | Remove símbolo de moeda antes da conversão numérica. |
| 300 | `        } catch (NumberFormatException e) {` | Captura erro de parsing numérico e usa valor padrão. |
| 301 | `            return def;` | Executa parte do fluxo descrito pelo bloco onde esta linha está inserida. |
| 302 | `        }` | Delimita o bloco de código da estrutura atual. |
| 303 | `    }` | Delimita o bloco de código da estrutura atual. |
| 304 | `` | Linha em branco para separar blocos lógicos. |
| 305 | `    private static int clamp(int v, int min, int max) {` | Força um número a permanecer entre mínimo e máximo. |
| 306 | `        return Math.max(min, Math.min(max, v));` | Implementa o limite inferior e superior do valor recebido. |
| 307 | `    }` | Delimita o bloco de código da estrutura atual. |
| 308 | `}` | Delimita o bloco de código da estrutura atual. |
