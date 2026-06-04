package br.com.steamcatalog.exception;

/**
 * Lancada quando um registro procurado nao existe na collection.
 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
