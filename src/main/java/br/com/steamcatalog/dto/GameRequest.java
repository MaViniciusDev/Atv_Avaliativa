package br.com.steamcatalog.dto;

import java.util.List;

import br.com.steamcatalog.model.Game;

/** Corpo das requisicoes de cadastro/alteracao de Game. */
public record GameRequest(
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

    /** Converte o corpo recebido no modelo de dominio (sem as referencias). */
    public Game toGame() {
        Game g = new Game();
        g.setAppid(appid);
        g.setName(name);
        g.setType((type == null || type.isBlank()) ? "game" : type);
        g.setReleaseDate(releaseDate);
        g.setPrice(price);
        g.setDescription(description);
        g.setThumbnail(thumbnail);
        g.setReviewScore(reviewScore);
        g.setPositiveReview(positiveReview);
        g.setNegativeReview(negativeReview);
        g.setRank(rank);
        g.setTags(tags);
        return g;
    }
}
