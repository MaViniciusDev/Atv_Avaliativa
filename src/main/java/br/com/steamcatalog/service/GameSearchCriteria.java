package br.com.steamcatalog.service;

/**
 * Agrupa os criterios de pesquisa de jogos. Todos sao opcionais e, quando
 * preenchidos, sao combinados em AND. Usa o padrao "fluent" para facilitar a
 * montagem da consulta a partir do menu.
 *
 * Ex.: new GameSearchCriteria().name("counter").maxPrice(0).minScore(8)
 */
public class GameSearchCriteria {

    private String name;          // parte do nome (case-insensitive)
    private String type;          // game, dlc, demo...
    private Double minPrice;
    private Double maxPrice;
    private Integer minScore;     // reviewScore minimo
    private String developerName; // resolvido para developerId no service
    private String publisherName; // resolvido para publisherId no service
    private String tag;           // tag exata (lista)
    private Integer maxRank;      // melhores posicoes (rank <= maxRank)
    private int limit = 20;

    public GameSearchCriteria name(String name) {
        this.name = name;
        return this;
    }

    public GameSearchCriteria type(String type) {
        this.type = type;
        return this;
    }

    public GameSearchCriteria minPrice(Double minPrice) {
        this.minPrice = minPrice;
        return this;
    }

    public GameSearchCriteria maxPrice(Double maxPrice) {
        this.maxPrice = maxPrice;
        return this;
    }

    public GameSearchCriteria minScore(Integer minScore) {
        this.minScore = minScore;
        return this;
    }

    public GameSearchCriteria developerName(String developerName) {
        this.developerName = developerName;
        return this;
    }

    public GameSearchCriteria publisherName(String publisherName) {
        this.publisherName = publisherName;
        return this;
    }

    public GameSearchCriteria tag(String tag) {
        this.tag = tag;
        return this;
    }

    public GameSearchCriteria maxRank(Integer maxRank) {
        this.maxRank = maxRank;
        return this;
    }

    public GameSearchCriteria limit(int limit) {
        this.limit = limit;
        return this;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public Double getMinPrice() {
        return minPrice;
    }

    public Double getMaxPrice() {
        return maxPrice;
    }

    public Integer getMinScore() {
        return minScore;
    }

    public String getDeveloperName() {
        return developerName;
    }

    public String getPublisherName() {
        return publisherName;
    }

    public String getTag() {
        return tag;
    }

    public Integer getMaxRank() {
        return maxRank;
    }

    public int getLimit() {
        return limit;
    }
}
