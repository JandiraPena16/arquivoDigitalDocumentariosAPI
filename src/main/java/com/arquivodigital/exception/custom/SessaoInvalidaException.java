package com.arquivodigital.exception.custom;

public class SessaoInvalidaException extends RuntimeException {
    public SessaoInvalidaException(String mensagem) {
        super(mensagem);
    }
}
