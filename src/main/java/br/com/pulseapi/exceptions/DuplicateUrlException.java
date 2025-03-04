package br.com.pulseapi.exceptions;

public class DuplicateUrlException extends RuntimeException {
    public DuplicateUrlException(String message) {
        super(message);
    }
}
