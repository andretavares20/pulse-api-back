package br.com.pulseapi.exceptions;

/**
 * Exceção lançada quando uma tentativa de registrar uma URL de API duplicada é detectada.
 * Indica que a URL fornecida já existe no sistema.
 */
public class DuplicateApiUrlException extends RuntimeException {

    private static final long serialVersionUID = 1L; // Recomendado pra exceções serializáveis

    /**
     * Constrói uma nova exceção com a mensagem especificada.
     *
     * @param message Mensagem descrevendo o erro (ex.: "Já existe um registro com a URL 'example.com'")
     */
    public DuplicateApiUrlException(String message) {
        super(message);
    }

    /**
     * Constrói uma nova exceção com a mensagem e a causa raiz.
     *
     * @param message Mensagem descrevendo o erro
     * @param cause   Causa raiz da exceção (ex.: outra exceção que levou a essa condição)
     */
    public DuplicateApiUrlException(String message, Throwable cause) {
        super(message, cause);
    }
}
