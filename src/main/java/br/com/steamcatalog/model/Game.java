package br.com.steamcatalog.model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Collection "games" - jogo da Steam.
 * Equivale a "Turma" do exemplo do enunciado: so pode ser cadastrado se o
 * Developer e o Publisher referenciados JA existirem em suas collections.
 *
 * Relacionamento (1:N): developerId e publisherId guardam o id (String) de um
 * documento das collections developers/publishers, respectivamente.
 */
@Document(collection = "games")
public class Game {

    @Id
    private String id;

    @Indexed(unique = true)
    private int appid;            // identificador unico do jogo na Steam

    private String name;
    private String type;          // game, dlc, demo, etc.
    private String releaseDate;   // ISO yyyy-MM-dd
    private double price;
    private String description;
    private String thumbnail;     // URL da capa
    private int reviewScore;      // 0 a 10
    private int positiveReview;
    private int negativeReview;
    private int rank;

    @Indexed
    private String developerId;   // FK -> developers.id

    @Indexed
    private String publisherId;   // FK -> publishers.id

    private List<String> tags = new ArrayList<>();

    public Game() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getAppid() {
        return appid;
    }

    public void setAppid(int appid) {
        this.appid = appid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public int getReviewScore() {
        return reviewScore;
    }

    public void setReviewScore(int reviewScore) {
        this.reviewScore = reviewScore;
    }

    public int getPositiveReview() {
        return positiveReview;
    }

    public void setPositiveReview(int positiveReview) {
        this.positiveReview = positiveReview;
    }

    public int getNegativeReview() {
        return negativeReview;
    }

    public void setNegativeReview(int negativeReview) {
        this.negativeReview = negativeReview;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public String getDeveloperId() {
        return developerId;
    }

    public void setDeveloperId(String developerId) {
        this.developerId = developerId;
    }

    public String getPublisherId() {
        return publisherId;
    }

    public void setPublisherId(String publisherId) {
        this.publisherId = publisherId;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = (tags == null) ? new ArrayList<>() : tags;
    }

    @Override
    public String toString() {
        return String.format("Game{appid=%d, name='%s', type='%s', price=%.2f, score=%d, rank=%d}",
                appid, name, type, price, reviewScore, rank);
    }
}
