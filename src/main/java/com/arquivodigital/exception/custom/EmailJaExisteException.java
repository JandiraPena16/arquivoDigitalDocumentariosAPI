package com.arquivodigital.exception.custom;

public class EmailJaExisteException extends RuntimeException {
    public EmailJaExisteException(String email) {
        super("Email já está em uso: " + email);
    }
}
