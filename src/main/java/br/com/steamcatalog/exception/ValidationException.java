package br.com.steamcatalog.exception;

/**
 * Lancada quando os dados informados para cadastro/alteracao sao invalidos
 * (campos obrigatorios em branco, valores fora de faixa, duplicidade, etc.).
 */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
