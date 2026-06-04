package br.com.steamcatalog.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Collection "developers" - desenvolvedora do jogo.
 * Equivale ao "Professor" do exemplo do enunciado: pode ser cadastrado
 * livremente, mas nao pode ser excluido se houver Game vinculado a ele.
 */
@Document(collection = "developers")
public class Developer {

    @Id
    private String id;

    @Indexed(unique = true)
    private String name;

    private String country; // pais de origem (opcional)

    public Developer() {
    }

    public Developer(String name) {
        this.name = name;
    }

    public Developer(String name, String country) {
        this.name = name;
        this.country = country;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    @Override
    public String toString() {
        return String.format("Developer{id=%s, name='%s', country='%s'}", id, name, country);
    }
}
