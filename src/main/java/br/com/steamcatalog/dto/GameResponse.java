package br.com.steamcatalog.dto;

import java.util.List;

import br.com.steamcatalog.model.Game;

/** Resposta JSON de Game, enriquecida com os NOMES de developer/publisher. */
public record GameResponse(
        int appid,
        String name,
        String type,
        String releaseDate,
        double price,
        String description,
        String thumbnail,
        int reviewScore,
        int positiveReview,
        int negativeReview,
        int rank,
        List<String> tags,
        String developer,
        String publisher) {

    public static GameResponse of(Game g, String developerName, String publisherName) {
        return new GameResponse(
                g.getAppid(), g.getName(), g.getType(), g.getReleaseDate(),
                g.getPrice(), g.getDescription(), g.getThumbnail(),
                g.getReviewScore(), g.getPositiveReview(), g.getNegativeReview(),
                g.getRank(), g.getTags(), developerName, publisherName);
    }
}
