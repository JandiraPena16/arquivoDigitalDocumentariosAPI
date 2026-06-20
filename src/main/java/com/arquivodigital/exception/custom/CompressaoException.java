package com.arquivodigital.exception.custom;

public class CompressaoException extends RuntimeException {
    public CompressaoException(String mensagem) {
        super(mensagem);
    }
    public CompressaoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
