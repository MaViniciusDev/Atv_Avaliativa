package br.com.steamcatalog.exception;

/**
 * Lancada quando uma operacao violaria a integridade referencial entre as
 * collections. Exemplos:
 *  - cadastrar um Game cujo Developer/Publisher ainda nao existe;
 *  - excluir um Developer/Publisher que possui Games vinculados.
 */
public class IntegrityException extends RuntimeException {
    public IntegrityException(String message) {
        super(message);
    }
}
