package br.com.steamcatalog.dto;

import java.util.List;

/** Envelope de listas: {"total": N, "items": [...]}. */
public record ListResponse(long total, List<?> items) {

    public static ListResponse of(List<?> items) {
        return new ListResponse(items.size(), items);
    }
}
