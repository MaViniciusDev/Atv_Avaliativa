package br.com.steamcatalog.dto;

import br.com.steamcatalog.model.Developer;
import br.com.steamcatalog.model.Publisher;

/** Resposta JSON de Developer/Publisher. */
public record EntityResponse(String id, String name, String country) {

    public static EntityResponse of(Developer d) {
        return new EntityResponse(d.getId(), d.getName(), d.getCountry());
    }

    public static EntityResponse of(Publisher p) {
        return new EntityResponse(p.getId(), p.getName(), p.getCountry());
    }
}
